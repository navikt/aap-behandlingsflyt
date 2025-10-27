package no.nav.aap.behandlingsflyt.forretningsflyt.steg.aktivitetsplikt

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.testutil.FakeBrevbestillingGateway
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBrevbestillingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.util.*

class VurderAktivitetsplikt11_7StegTest {

    private lateinit var sak: Sak
    private lateinit var behandling: Behandling
    private lateinit var kontekst: FlytKontekstMedPerioder

    @BeforeEach
    fun setUp() {
        sak = InMemorySakRepository.finnEllerOpprett(
            Person(
                identifikator = UUID.randomUUID(),
                identer = listOf(Ident("12345678901")),
            ), Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1))
        )
        behandling = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Aktivitetsplikt,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.AKTIVITETSPLIKT_11_7)),
                årsak = ÅrsakTilOpprettelse.AKTIVITETSPLIKT,
            )
        )

        kontekst = opprettFlyktKontekst(sak = sak, behandlingId = behandling.id)
    }

    @Test
    fun `skal gi avklaringsbehov VURDER_AKTIVITETSPLIKT dersom dette ikke er håndtert aktivitetsplikt`() {
        val steg = opprettVurderAktivitetsplikt11_7Steg()

        steg.utfør(kontekst)
        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        assertThat(avklaringsbehovene.åpne()).hasSize(1)
        assertThat(avklaringsbehovene.åpne().first().definisjon).isEqualTo(Definisjon.VURDER_BRUDD_11_7)
    }

    @Test
    fun `skal gi fullført dersom aktivitetsplikten er oppfylt`() {
        val steg = opprettVurderAktivitetsplikt11_7Steg()
        steg.utfør(kontekst) // Opprette avklaringsbehovene
        InMemoryAktivitetsplikt11_7Repository.lagre(
            kontekst.behandlingId, listOf(
                Aktivitetsplikt11_7Vurdering(
                    begrunnelse = "Ok",
                    erOppfylt = true,
                    utfall = null,
                    vurdertAv = "A987652",
                    gjelderFra = kontekst.rettighetsperiode.fom.plusDays(10),
                    opprettet = Instant.now(),
                    vurdertIBehandling = kontekst.behandlingId,
                    skalIgnorereVarselFrist = false
                )
            )
        )
        val avklaringsbehov = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        avklaringsbehov.løsAvklaringsbehov(
            definisjon = Definisjon.VURDER_BRUDD_11_7,
            begrunnelse = "Vurdert ok",
            endretAv = "Ident"
        )

        steg.utfør(kontekst)
        val oppdatertAvklaringsbehov = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        assertThat(oppdatertAvklaringsbehov.åpne()).hasSize(0)
        assertThat(oppdatertAvklaringsbehov.alle()).hasSize(1)
    }

    @Test
    fun `skal vente på forhåndsvarsel dersom aktivitetsplikten ikke er oppfylt`() {
        val steg = opprettVurderAktivitetsplikt11_7Steg()
        steg.utfør(kontekst)
        opprettAktivitetspliktIkkeOppfyltVurdering()

        steg.utfør(kontekst)
        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        assertThat(avklaringsbehovene.åpne()).hasSize(1)
        assertThat(avklaringsbehovene.åpne().first().definisjon).isEqualTo(Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV)

        ferdigstillSkrivForhåndsvarsel()

        val resultat = steg.utfør(kontekst)
        assertThat(resultat).isInstanceOf(FantVentebehov::class.java)
    }

    @Test
    fun `skal hoppe over venting på forhåndsvarsel dersom aktivitetsplikten ikke er oppfylt men venting overstyrt av saksbehandler`() {
        val steg = opprettVurderAktivitetsplikt11_7Steg()
        steg.utfør(kontekst)
        opprettAktivitetspliktIkkeOppfyltVurdering()

        var resultat = steg.utfør(kontekst)
        assertThat(resultat).isEqualTo(Fullført)

        ferdigstillSkrivForhåndsvarsel()

        resultat = steg.utfør(kontekst)
        assertThat(resultat).isInstanceOf(FantVentebehov::class.java)

        val eksisterendeVurdering =
            InMemoryAktivitetsplikt11_7Repository.hentHvisEksisterer(kontekst.behandlingId)?.vurderinger?.find { it.vurdertIBehandling == kontekst.behandlingId }
                ?: error("Mangler vurdering for behandlingen")
        val oppdatertVurderingIgnorererFrist = eksisterendeVurdering.copy(skalIgnorereVarselFrist = true)
        InMemoryAktivitetsplikt11_7Repository.lagre(kontekst.behandlingId, listOf(oppdatertVurderingIgnorererFrist))
        resultat = steg.utfør(kontekst)
        assertThat(resultat).isEqualTo(Fullført)

        val avklaringsbehov = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        assertThat(avklaringsbehov.åpne()).hasSize(0)

    }

    @Test
    fun `skal hoppe over venting på forhåndsvarsel dersom aktivitetsplikten ikke er oppfylt men fristen er passert`() {
        val steg = opprettVurderAktivitetsplikt11_7Steg()
        steg.utfør(kontekst)
        opprettAktivitetspliktIkkeOppfyltVurdering()

        steg.utfør(kontekst)
        val avklaringsbehov = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        assertThat(avklaringsbehov.åpne()).hasSize(1)
        assertThat(avklaringsbehov.åpne().first().definisjon).isEqualTo(Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV)

        ferdigstillSkrivForhåndsvarsel()

        var resultat = steg.utfør(kontekst)
        assertThat(resultat).isInstanceOf(FantVentebehov::class.java)
        InMemoryAktivitetsplikt11_7Repository.lagreFrist(
            kontekst.behandlingId,
            LocalDate.now(),
            LocalDate.now().minusDays(1)
        )
        resultat = steg.utfør(kontekst)
        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `skal slette forhåndsvarslet brev dersom man endrer til oppfylt`() {
        val steg = opprettVurderAktivitetsplikt11_7Steg()
        steg.utfør(kontekst)
        opprettAktivitetspliktIkkeOppfyltVurdering()

        steg.utfør(kontekst)
        var avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        assertThat(avklaringsbehovene.åpne()).hasSize(1)
        assertThat(avklaringsbehovene.åpne().first().definisjon).isEqualTo(Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV)
        opprettAvklaringsbehovForÅSkriveForhåndsvarsel()

        val eksisterendeVurdering =
            InMemoryAktivitetsplikt11_7Repository.hentHvisEksisterer(kontekst.behandlingId)
                ?.vurderinger?.find { it.vurdertIBehandling == kontekst.behandlingId }
                ?: error("Mangler vurdering for behandlingen")
        val oppdatertVurderingOppfylt = eksisterendeVurdering.copy(erOppfylt = true, utfall = null)
        InMemoryAktivitetsplikt11_7Repository.lagre(kontekst.behandlingId, listOf(oppdatertVurderingOppfylt))
        steg.utfør(kontekst)
        avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        assertThat(avklaringsbehovene.åpne()).hasSize(0)

        val avklaringsbehovForhåndsvarselOppdatert = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV)
        assertThat(avklaringsbehovForhåndsvarselOppdatert?.status()?.erAvsluttet()).isTrue
    }


    private fun opprettAktivitetspliktIkkeOppfyltVurdering() {
        val avklaringsbehov = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        avklaringsbehov.løsAvklaringsbehov(
            definisjon = Definisjon.VURDER_BRUDD_11_7,
            begrunnelse = "Vurdert ok",
            endretAv = "Ident"
        )

        InMemoryAktivitetsplikt11_7Repository.lagre(
            kontekst.behandlingId, listOf(
                Aktivitetsplikt11_7Vurdering(
                    begrunnelse = "Ok",
                    erOppfylt = false,
                    utfall = Utfall.STANS,
                    vurdertAv = "A987652",
                    gjelderFra = kontekst.rettighetsperiode.fom.plusDays(10),
                    opprettet = Instant.now(),
                    vurdertIBehandling = kontekst.behandlingId,
                    skalIgnorereVarselFrist = false
                )
            )
        )
    }

    private fun ferdigstillSkrivForhåndsvarsel() {
        opprettAvklaringsbehovForÅSkriveForhåndsvarsel()
        val avklaringsbehov = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        avklaringsbehov.løsAvklaringsbehov(
            definisjon = Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV,
            begrunnelse = "Vurdert ok",
            endretAv = "Ident"
        )
        val brevbestillingReferanse = BrevbestillingReferanse(UUID.randomUUID())
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = kontekst.behandlingId,
            typeBrev = TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
            bestillingReferanse = brevbestillingReferanse,
            status = Status.FULLFØRT
        )
        InMemoryAktivitetsplikt11_7Repository.lagreVarsel(kontekst.behandlingId, brevbestillingReferanse)
        InMemoryAktivitetsplikt11_7Repository.lagreFrist(
            kontekst.behandlingId,
            LocalDate.now(),
            LocalDate.now().plusDays(21)
        )
    }


    private fun opprettAvklaringsbehovForÅSkriveForhåndsvarsel() {
        InMemoryAvklaringsbehovRepository.opprett(
            kontekst.behandlingId,
            definisjon = Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV,
            funnetISteg = StegType.VURDER_AKTIVITETSPLIKT_11_7,
            frist = null,
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )
    }


    private fun opprettFlyktKontekst(sak: Sak, behandlingId: BehandlingId): FlytKontekstMedPerioder =
        FlytKontekstMedPerioder(
            sakId = sak.id,
            behandlingId = behandlingId,
            behandlingType = TypeBehandling.Aktivitetsplikt,
            forrigeBehandlingId = null,
            vurderingType = VurderingType.IKKE_RELEVANT,
            rettighetsperiode = sak.rettighetsperiode,
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.AKTIVITETSPLIKT_11_7)
        )

    private fun opprettVurderAktivitetsplikt11_7Steg(): VurderAktivitetsplikt11_7Steg =
        VurderAktivitetsplikt11_7Steg.konstruer(
            inMemoryRepositoryProvider,
            createGatewayProvider {
                register<FakeUnleash>()
                register<FakeBrevbestillingGateway>()
            }
        )
}