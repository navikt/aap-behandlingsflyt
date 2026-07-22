package no.nav.aap.behandlingsflyt.behandling.brev

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Brevbestilling
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.brev.kontrakt.Rolle
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.oppgave.enhet.OppgaveEnhetDto
import no.nav.aap.oppgave.enhet.OppgaveEnhetResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.*
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus

class SignaturServiceTest {

    private val oppgavestyringGateway = mockk<OppgavestyringGateway>()
    private val behandlingRepository = InMemoryBehandlingRepository
    private val avklaringsbehovRepository = InMemoryAvklaringsbehovRepository
    private val avklaringsbehovOperasjonerRepository = InMemoryAvklaringsbehovRepository
    private val signaturService =
        SignaturService(oppgavestyringGateway, behandlingRepository, avklaringsbehovRepository)

    private val behandlingTilOppgaveEnhet = mutableMapOf<BehandlingReferanse, List<OppgaveEnhetDto>>()

    @BeforeEach
    fun setup() {
        val behandlingReferanse = slot<BehandlingReferanse>()
        every { oppgavestyringGateway.hentOppgaveEnhet(capture(behandlingReferanse)) } answers {
            OppgaveEnhetResponse(behandlingTilOppgaveEnhet[behandlingReferanse.captured] ?: emptyList())
        }
    }

    @Test
    fun `den som står i signatur for en gitt rolle er den som utførte siste avklaringsbehovet for rollen (status AVSLUTTET)`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val brevbestilling = Brevbestilling(
            id = 0,
            behandlingId = behandling.id,
            typeBrev = TypeBrev.VEDTAK_INNVILGELSE,
            referanse = BrevbestillingReferanse(UUID.randomUUID()),
            status = Status.FORHÅNDSVISNING_KLAR,
            opprettet = LocalDateTime.now()
        )
        val veilederIdent = "v000000"
        val kvalitetssikrerIdent = "k000000"
        val saksbehandlerIdent = "s000000"
        val beslutterIdent = "b000000"

        // SAKSBEHANDLER_OPPFOLGING
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.FRITAK_MELDEPLIKT,
            endretAv = veilederIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "1234"
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.AVKLAR_SYKDOM,
            endretAv = SYSTEMBRUKER.ident,
            status = AvklaringsbehovStatus.OPPRETTET,
            oppgaveEnhet = "2345"
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.AVKLAR_SYKDOM,
            endretAv = veilederIdent,
            status = AvklaringsbehovStatus.AVSLUTTET
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.AVKLAR_SYKDOM,
            endretAv = kvalitetssikrerIdent,
            status = AvklaringsbehovStatus.KVALITETSSIKRET
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.AVKLAR_SYKDOM,
            endretAv = beslutterIdent,
            status = AvklaringsbehovStatus.TOTRINNS_VURDERT
        )

        // KVALITETSSIKRER
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.KVALITETSSIKRING,
            endretAv = kvalitetssikrerIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "3456"
        )

        // SAKSBEHANDLER_NASJONAL
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.AVKLAR_STUDENT,
            endretAv = saksbehandlerIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "4567"
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT,
            endretAv = saksbehandlerIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "5678"
        )

        // definisjon som ikke skal tas høyde for
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            endretAv = saksbehandlerIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "6789"
        )

        // definisjon som ikke skal tas høyde for
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.VURDER_TREKK_AV_SØKNAD,
            endretAv = saksbehandlerIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "7890"
        )

        // definisjon som ikke skal tas høyde for
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.VURDER_TREKK_AV_SØKNAD,
            endretAv = saksbehandlerIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "7891"
        )

        // BESLUTTER
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.FATTE_VEDTAK,
            endretAv = beslutterIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "8901"
        )

        val signaturer = signaturService.finnSignaturGrunnlag(brevbestilling, Bruker(""))

        assertThat(signaturer).containsExactly( // NB: Tester også rekkefølge.
            SignaturGrunnlag(navIdent = beslutterIdent, rolle = Rolle.BESLUTTER, enhet = "8901"),
            SignaturGrunnlag(navIdent = saksbehandlerIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL, enhet = "5678"),
            SignaturGrunnlag(navIdent = kvalitetssikrerIdent, rolle = Rolle.KVALITETSSIKRER, enhet = "3456"),
            SignaturGrunnlag(navIdent = veilederIdent, rolle = Rolle.SAKSBEHANDLER_OPPFOLGING, enhet = "2345")
        )
    }

    @Test
    fun `skal fjerne duplikater om samme person har løst avklaringsbehov som er knytte til ulike roller – eksempel samme person på lokalkontor og NAY`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val brevbestilling = Brevbestilling(
            id = 0,
            behandlingId = behandling.id,
            typeBrev = TypeBrev.VEDTAK_INNVILGELSE,
            referanse = BrevbestillingReferanse(UUID.randomUUID()),
            status = Status.FORHÅNDSVISNING_KLAR,
            opprettet = LocalDateTime.now()
        )
        val veilederIdent = "v000000"
        val kvalitetssikrerIdent = "k000000"
        val beslutterIdent = "b000000"

        // SAKSBEHANDLER_OPPFOLGING
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.FRITAK_MELDEPLIKT,
            endretAv = veilederIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "1234"
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.AVKLAR_SYKDOM,
            endretAv = SYSTEMBRUKER.ident,
            status = AvklaringsbehovStatus.OPPRETTET,
            oppgaveEnhet = "2345"
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.AVKLAR_SYKDOM,
            endretAv = veilederIdent,
            status = AvklaringsbehovStatus.AVSLUTTET
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.AVKLAR_SYKDOM,
            endretAv = kvalitetssikrerIdent,
            status = AvklaringsbehovStatus.KVALITETSSIKRET
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.AVKLAR_SYKDOM,
            endretAv = beslutterIdent,
            status = AvklaringsbehovStatus.TOTRINNS_VURDERT
        )

        // SAKSBEHANDLER_NASJONAL
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.AVKLAR_STUDENT,
            endretAv = veilederIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "3456"
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT,
            endretAv = veilederIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "4567"
        )

        // KVALITETSSIKRER
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.KVALITETSSIKRING,
            endretAv = kvalitetssikrerIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "5678"
        )

        // BESLUTTER
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.FATTE_VEDTAK,
            endretAv = beslutterIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "6789"
        )

        val signaturer = signaturService.finnSignaturGrunnlag(brevbestilling, Bruker(""))

        assertThat(signaturer).hasSize(3)
        assertThat(signaturer).containsExactlyInAnyOrder(
            SignaturGrunnlag(navIdent = beslutterIdent, rolle = Rolle.BESLUTTER, enhet = "6789"),
            SignaturGrunnlag(navIdent = kvalitetssikrerIdent, rolle = Rolle.KVALITETSSIKRER, enhet = "5678"),
            SignaturGrunnlag(navIdent = veilederIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL, enhet = "4567")
        )
    }

    @Test
    fun `tar med innlogget bruker i signatur for vedtaksbrev dersom beslutter ikke har saksbehandlet`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val brevbestilling = Brevbestilling(
            id = 0,
            behandlingId = behandling.id,
            typeBrev = TypeBrev.VEDTAK_INNVILGELSE,
            referanse = BrevbestillingReferanse(UUID.randomUUID()),
            status = Status.FORHÅNDSVISNING_KLAR,
            opprettet = LocalDateTime.now()
        )
        val saksbehandlerIdent = "s000000"
        val innloggetBrukerIdent = "i000000"

        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT,
            endretAv = saksbehandlerIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "1234"
        )

        // BESLUTTER
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.SKRIV_VEDTAKSBREV,
            endretAv = SYSTEMBRUKER.ident,
            status = AvklaringsbehovStatus.OPPRETTET,
            oppgaveEnhet = "2345"
        )

        val signaturer = signaturService.finnSignaturGrunnlag(brevbestilling, Bruker(innloggetBrukerIdent))

        assertThat(signaturer).containsExactlyInAnyOrder(
            SignaturGrunnlag(navIdent = innloggetBrukerIdent, rolle = Rolle.BESLUTTER, enhet = "2345"),
            SignaturGrunnlag(navIdent = saksbehandlerIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL, enhet = "1234")
        )
    }

    @Test
    fun `dersom beslutter ikke har saksbehandlet og innlogget bruker har saksbehandlet beholdes rollen til innlogget bruker`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val brevbestilling = Brevbestilling(
            id = 0,
            behandlingId = behandling.id,
            typeBrev = TypeBrev.KLAGE_OPPRETTHOLDELSE,
            referanse = BrevbestillingReferanse(UUID.randomUUID()),
            status = Status.FORHÅNDSVISNING_KLAR,
            opprettet = LocalDateTime.now()
        )
        val veilederIdent = "v000000"
        val kvalitetssikrerIdent = "k000000"
        val saksbehandlerIdent = "s000000"

        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.FASTSETT_PÅKLAGET_BEHANDLING,
            endretAv = saksbehandlerIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "1234"
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.FASTSETT_FULLMEKTIG,
            endretAv = saksbehandlerIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "1234"
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.VURDER_FORMKRAV,
            endretAv = saksbehandlerIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "1234"
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.FASTSETT_BEHANDLENDE_ENHET,
            endretAv = saksbehandlerIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "1234"
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.VURDER_KLAGE_KONTOR,
            endretAv = veilederIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "2345"
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.KVALITETSSIKRING,
            endretAv = kvalitetssikrerIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "3456"
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.VURDER_KLAGE_KONTOR,
            endretAv = veilederIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "2345"
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.KVALITETSSIKRING,
            endretAv = kvalitetssikrerIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "3456"
        )
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.SKRIV_VEDTAKSBREV,
            endretAv = SYSTEMBRUKER.ident,
            status = AvklaringsbehovStatus.OPPRETTET,
            oppgaveEnhet = "4567"
        )
        val signaturer = signaturService.finnSignaturGrunnlag(brevbestilling, Bruker(kvalitetssikrerIdent))
        assertThat(signaturer).containsExactly(
            SignaturGrunnlag(navIdent = saksbehandlerIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL, enhet = "1234"),
            SignaturGrunnlag(navIdent = kvalitetssikrerIdent, rolle = Rolle.KVALITETSSIKRER, enhet = "3456"),
            SignaturGrunnlag(navIdent = veilederIdent, rolle = Rolle.SAKSBEHANDLER_OPPFOLGING, enhet = "2345"),
        )
    }

    @Test
    fun `innlogget bruker i signatur dersom det ikke er et vedtaksbrev`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val brevbestilling = Brevbestilling(
            id = 0,
            behandlingId = behandling.id,
            typeBrev = TypeBrev.FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT,
            referanse = BrevbestillingReferanse(UUID.randomUUID()),
            status = Status.FORHÅNDSVISNING_KLAR,
            opprettet = LocalDateTime.now()
        )
        val innloggetBrukerIdent = "i000000"

        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.SKRIV_VEDTAKSBREV,
            endretAv = SYSTEMBRUKER.ident,
            status = AvklaringsbehovStatus.OPPRETTET,
            oppgaveEnhet = "1234"
        )

        val signaturer = signaturService.finnSignaturGrunnlag(brevbestilling, Bruker(innloggetBrukerIdent))

        assertThat(signaturer).containsExactly(
            SignaturGrunnlag(navIdent = innloggetBrukerIdent, rolle = null, enhet = "1234")
        )
    }

    @Test
    fun `gir signatur men uten enhet dersom oppgave ikke har enhet for definisjon på avklaringsbehovet`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val brevbestilling = Brevbestilling(
            id = 0,
            behandlingId = behandling.id,
            typeBrev = TypeBrev.VEDTAK_11_17,
            referanse = BrevbestillingReferanse(UUID.randomUUID()),
            status = Status.FORHÅNDSVISNING_KLAR,
            opprettet = LocalDateTime.now()
        )
        val beslutterIdent = "b000000"

        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.FATTE_VEDTAK,
            endretAv = beslutterIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = null
        )

        val signaturer = signaturService.finnSignaturGrunnlag(brevbestilling, Bruker(""))

        assertThat(signaturer).containsExactly(
            SignaturGrunnlag(navIdent = beslutterIdent, rolle = Rolle.BESLUTTER, enhet = null)
        )
    }

    @Test
    fun `SKRIV_VEDTAKSBREV_SAKSBEHANDLER gir saksbehandler nasjonal som brevskriver uten beslutter`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val brevbestilling = Brevbestilling(
            id = 0,
            behandlingId = behandling.id,
            typeBrev = TypeBrev.KLAGE_OPPRETTHOLDELSE,
            referanse = BrevbestillingReferanse(UUID.randomUUID()),
            status = Status.FORHÅNDSVISNING_KLAR,
            opprettet = LocalDateTime.now()
        )
        val veilederIdent = "v000000"
        val kvalitetssikrerIdent = "k000000"
        val saksbehandlerIdent = "s000000"

        // SAKSBEHANDLER_OPPFOLGING
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.VURDER_KLAGE_KONTOR,
            endretAv = veilederIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "1234"
        )

        // SAKSBEHANDLER_NASJONAL
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.FASTSETT_PÅKLAGET_BEHANDLING,
            endretAv = saksbehandlerIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "2345"
        )

        // KVALITETSSIKRER
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.KVALITETSSIKRING,
            endretAv = kvalitetssikrerIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "3456"
        )

        // SAKSBEHANDLER_NASJONAL skriver vedtaksbrev (ikke beslutter)
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.SKRIV_VEDTAKSBREV_SAKSBEHANDLER,
            endretAv = saksbehandlerIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "4567"
        )

        val signaturer = signaturService.finnSignaturGrunnlag(brevbestilling, Bruker(""))

        assertThat(signaturer).containsExactly(
            SignaturGrunnlag(navIdent = saksbehandlerIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL, enhet = "4567"),
            SignaturGrunnlag(navIdent = kvalitetssikrerIdent, rolle = Rolle.KVALITETSSIKRER, enhet = "3456"),
            SignaturGrunnlag(navIdent = veilederIdent, rolle = Rolle.SAKSBEHANDLER_OPPFOLGING, enhet = "1234"),
        )
        // Verifiser at det ikke er noen BESLUTTER-signatur
        assertThat(signaturer.none { it.rolle == Rolle.BESLUTTER }).isTrue()
    }

    private fun leggTilEndring(
        behandling: Behandling,
        definisjon: Definisjon,
        endretAv: String,
        status: AvklaringsbehovStatus,
        oppgaveEnhet: String? = null,
    ) {
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovOperasjonerRepository, behandling.id)
        val bruker = Bruker(endretAv)

        avklaringsbehovene.leggTil(
            definisjon = definisjon,
            funnetISteg = definisjon.løsesISteg,
            perioderSomIkkeErTilstrekkeligVurdert = null,
            perioderVedtaketBehøverVurdering = null,
            begrunnelse = "...",
            bruker = bruker
        )

        when (status) {
            AvklaringsbehovStatus.OPPRETTET -> Unit
            AvklaringsbehovStatus.AVSLUTTET -> {
                avklaringsbehovene.løsAvklaringsbehov(definisjon, "...", bruker)
            }

            AvklaringsbehovStatus.KVALITETSSIKRET -> {
                val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
                if (avklaringsbehov != null && !avklaringsbehov.harAvsluttetStatusIHistorikken()) {
                    avklaringsbehovene.løsAvklaringsbehov(definisjon, "...", bruker)
                }
                avklaringsbehovene.vurderKvalitet(definisjon, godkjent = true, begrunnelse = "...", vurdertAv = bruker)
            }

            AvklaringsbehovStatus.TOTRINNS_VURDERT -> {
                val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
                if (avklaringsbehov != null && !avklaringsbehov.harAvsluttetStatusIHistorikken()) {
                    avklaringsbehovene.løsAvklaringsbehov(definisjon, "...", bruker)
                }
                avklaringsbehovene.vurderTotrinn(definisjon, godkjent = true, begrunnelse = "...", vurdertAv = bruker)
            }

            else -> error("Ustøttet status i testdata: $status")
        }

        if (oppgaveEnhet != null) {
            val eksistende = behandlingTilOppgaveEnhet[behandling.referanse] ?: emptyList()
            behandlingTilOppgaveEnhet[behandling.referanse] =
                eksistende
                    .filterNot { it.avklaringsbehovKode == definisjon.kode.name }
                    .plus(OppgaveEnhetDto(definisjon.kode.name, oppgaveEnhet))
        }
    }

}
