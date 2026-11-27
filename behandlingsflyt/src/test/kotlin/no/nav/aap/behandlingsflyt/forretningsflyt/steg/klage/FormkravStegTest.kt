package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.brev.ForhåndsvarselKlageFormkrav
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.Ventebehov
import no.nav.aap.behandlingsflyt.flyt.testutil.FakeBrevbestillingGateway
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgKlage
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.FakeOppgavestyringGateway
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBrevbestillingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryFormkravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus

@Execution(ExecutionMode.SAME_THREAD)
class FormkravStegTest {
    private val klage = opprettInMemorySakOgKlage().second
    private val gatewayProvider = minimalGatewayProvider {
        register<AlleAvskruddUnleash>()
        register<FakeOppgavestyringGateway>()
        register<FakeBrevbestillingGateway>()
    }
    private val brevbestillingService = BrevbestillingService(inMemoryRepositoryProvider, gatewayProvider)
    private val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(klage.id)
    private val formkravRepository = InMemoryFormkravRepository
    private val steg = FormkravSteg.konstruer(inMemoryRepositoryProvider, gatewayProvider)
    private val kontekst = flytKontekstMedPerioder {
        behandling = klage
    }
    private val vurdering = FormkravVurdering(
        begrunnelse = "Vurdering av formkrav",
        erBrukerPart = true,
        erFristOverholdt = true,
        likevelBehandles = null,
        erKonkret = true,
        erSignert = true,
        vurdertAv = Bruker("saksbehandler"),
        opprettet = Instant.now()
    )

//    }

    @Test
    fun `oppretter avklaringsbehov for formkrav når vurderingen mangler`() {
        steg.utfør(kontekst)
        steg.utfør(kontekst)

        assertThat(InMemoryAvklaringsbehovRepository.hent(klage.id).map { it.definisjon })
            .containsExactly(Definisjon.VURDER_FORMKRAV)
        assertThat(avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VURDER_FORMKRAV)?.status())
            .isEqualTo(AvklaringsbehovStatus.OPPRETTET)
        assertThat(InMemoryBrevbestillingRepository.hent(klage.id)).isEmpty()
    }

    @Test
    fun `fullfører når formkravene er oppfylt og avklaringsbehovet er løst`() {
        løsFormkrav(vurdering)
        bestillForhåndsvarsel(Status.FULLFØRT)

        val resultat = steg.utfør(kontekst)

        assertThat(resultat).isEqualTo(Fullført)
        assertThat(avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VURDER_FORMKRAV)?.status())
            .isEqualTo(AvklaringsbehovStatus.AVSLUTTET)
        assertThat(avklaringsbehovene.hentBehovForDefinisjon(Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV))
            .isNull()
    }

    @Test
    fun `fullfører uten forhåndsvarsel når klagefristen ikke er overholdt`() {
        løsFormkrav(vurdering.copy(erSignert = false, erFristOverholdt = false, likevelBehandles = false))

        val resultat = steg.utfør(kontekst)

        assertThat(resultat).isEqualTo(Fullført)
        assertThat(avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VURDER_FORMKRAV)?.status())
            .isEqualTo(AvklaringsbehovStatus.AVSLUTTET)
        assertThat(avklaringsbehovene.hentBehovForDefinisjon(Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV))
            .isNull()
        assertThat(InMemoryBrevbestillingRepository.hent(klage.id)).isEmpty()
    }

    @Test
    fun `bestiller ett forhåndsvarsel og oppretter avklaringsbehov når formkravene ikke er oppfylt`() {
        løsFormkrav(vurdering.copy(erKonkret = false, erBrukerPart = false, erSignert = false))

        steg.utfør(kontekst)
        steg.utfør(kontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(
            Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV
        )
        assertThat(avklaringsbehov?.status()).isEqualTo(AvklaringsbehovStatus.OPPRETTET)
        val bestilling = InMemoryBrevbestillingRepository.hent(klage.id).single()
        assertThat(bestilling.typeBrev).isEqualTo(TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV)
        assertThat(bestilling.status).isEqualTo(Status.FORHÅNDSVISNING_KLAR)
        assertThat(formkravRepository.hentHvisEksisterer(klage.id)?.varsel?.varselId)
            .isEqualTo(bestilling.referanse)
    }

    @Test
    fun `beholder avklaringsbehov og gjenbruker forhåndsvarsel som ikke er sendt`() {
        løsFormkrav(vurdering.copy(erKonkret = false, erSignert = false))
        val referanse = bestillForhåndsvarsel(Status.FORHÅNDSVISNING_KLAR)
        formkravRepository.lagreVarsel(klage.id, referanse)
        steg.utfør(kontekst)

        steg.utfør(kontekst)

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(
            Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV
        )
        assertThat(avklaringsbehov?.status()).isEqualTo(AvklaringsbehovStatus.OPPRETTET)
        assertThat(InMemoryBrevbestillingRepository.hent(klage.id).map { it.referanse })
            .containsExactly(referanse)
    }

    @Test
    fun `venter på svar og gjenåpner formkrav når forhåndsvarsel er sendt og formkravene ikke er oppfylt`() {
        løsFormkrav(
            vurdering.copy(erKonkret = false, erBrukerPart = false, erSignert = false, likevelBehandles = false)
        )
        val referanse = bestillForhåndsvarsel(Status.FULLFØRT)
        val svarfrist = LocalDate.now().plusWeeks(3)
        formkravRepository.lagreVarsel(klage.id, referanse)
        formkravRepository.lagreFrist(klage.id, LocalDate.now(), svarfrist)

        val resultat = steg.utfør(kontekst)

        assertThat(resultat).isEqualTo(
            FantVentebehov(
                Ventebehov(
                    definisjon = Definisjon.VENTE_PÅ_FRIST_FORHÅNDSVARSEL_KLAGE_FORMKRAV,
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_SVAR_FRA_BRUKER,
                    frist = svarfrist
                )
            )
        )
        assertThat(avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VURDER_FORMKRAV)?.status())
            .isEqualTo(AvklaringsbehovStatus.OPPRETTET)
    }

    @Test
    fun `fullfører etter svarfristen selv om formkravene ikke er oppfylt`() {
        løsFormkrav(
            vurdering.copy(erKonkret = false, erBrukerPart = false, erSignert = false, likevelBehandles = false)
        )
        val referanse = bestillForhåndsvarsel(Status.FULLFØRT)
        val sendtDato = LocalDate.now().minusWeeks(4)
        formkravRepository.lagreVarsel(klage.id, referanse)
        formkravRepository.lagreFrist(klage.id, sendtDato, sendtDato.plusWeeks(3))

        val resultat = steg.utfør(kontekst)

        assertThat(resultat).isEqualTo(Fullført)
        assertThat(avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VURDER_FORMKRAV)?.status())
            .isEqualTo(AvklaringsbehovStatus.AVSLUTTET)
    }

    private fun løsFormkrav(vurdering: FormkravVurdering) {
        steg.utfør(kontekst)
        formkravRepository.lagre(klage.id, vurdering)
        avklaringsbehovene.løsAvklaringsbehov(
            definisjon = Definisjon.VURDER_FORMKRAV,
            begrunnelse = vurdering.begrunnelse,
            endretAv = vurdering.vurdertAv
        )
    }

    private fun bestillForhåndsvarsel(status: Status): BrevbestillingReferanse {
        val referanse = BrevbestillingReferanse(
            brevbestillingService.bestill(
                behandlingId = klage.id,
                brevBehov = ForhåndsvarselKlageFormkrav,
                unikReferanse = UUID.randomUUID().toString(),
                ferdigstillAutomatisk = false
            )
        )
        InMemoryBrevbestillingRepository.oppdaterStatus(klage.id, referanse, status)
        return referanse
    }
}
