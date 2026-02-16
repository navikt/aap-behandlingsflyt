package no.nav.aap.behandlingsflyt.behandling.brev

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOperasjonerRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Endring
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Brevbestilling
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.brev.kontrakt.Rolle
import no.nav.aap.brev.kontrakt.SignaturGrunnlag
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.oppgave.enhet.OppgaveEnhetDto
import no.nav.aap.oppgave.enhet.OppgaveEnhetResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status as AvklaringsbehovStatus

@ExtendWith(MockKExtension::class)
@MockKExtension.RequireParallelTesting
@ParameterizedClass
@ValueSource(booleans = [true, false])
class SignaturServiceTest(
    // Midlertidig i overgang til ny implementasjon bak feature-toggle
    val hentEnhetFraOppgave: Boolean
) {


    private val unleashGateway = mockk<UnleashGateway>()
    private val oppgavestyringGateway = mockk<OppgavestyringGateway>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val avklaringsbehovRepository = mockk<AvklaringsbehovRepository>()
    private val avklaringsbehovOperasjonerRepository = mockk<AvklaringsbehovOperasjonerRepository>()
    private val signaturService =
        SignaturService(unleashGateway, oppgavestyringGateway, behandlingRepository, avklaringsbehovRepository)

    private val behandlingTilAvklaringsbehovene = mutableMapOf<BehandlingId, List<Avklaringsbehov>>()
    private val behandlingTilOppgaveEnhet = mutableMapOf<BehandlingReferanse, List<OppgaveEnhetDto>>()

    @BeforeEach
    fun setup() {
        every { unleashGateway.isEnabled(BehandlingsflytFeature.SignaturEnhetFraOppgave) } returns hentEnhetFraOppgave
        val behandlingId = slot<BehandlingId>()
        every { avklaringsbehovOperasjonerRepository.hent(capture(behandlingId)) } answers {
            behandlingTilAvklaringsbehovene.getValue(behandlingId.captured)
        }
        every { avklaringsbehovRepository.hentAvklaringsbehovene(capture(behandlingId)) } answers {
            Avklaringsbehovene(
                avklaringsbehovOperasjonerRepository,
                behandlingId.captured
            )
        }

        val behandlingReferanse = slot<BehandlingReferanse>()
        every { oppgavestyringGateway.hentOppgaveEnhet(capture(behandlingReferanse)) } answers {
            OppgaveEnhetResponse(behandlingTilOppgaveEnhet[behandlingReferanse.captured] ?: emptyList())
        }
    }

    @Test
    fun `den som står i signatur for en gitt rolle er den som utførte siste avklaringsbehovet for rollen (status AVSLUTTET)`() {
        val behandling = gittBehandling()
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

        // BESLUTTER
        leggTilEndring(
            behandling = behandling,
            definisjon = Definisjon.FATTE_VEDTAK,
            endretAv = beslutterIdent,
            status = AvklaringsbehovStatus.AVSLUTTET,
            oppgaveEnhet = "8901"
        )

        val signaturer = signaturService.finnSignaturGrunnlag(brevbestilling, Bruker(""))

        if (hentEnhetFraOppgave) { // Identisk assert i tillegg til å sjekke enhet
            assertThat(signaturer).containsExactly( // NB: Tester også rekkefølge.
                SignaturGrunnlag(navIdent = beslutterIdent, rolle = Rolle.BESLUTTER, enhet = "8901"),
                SignaturGrunnlag(navIdent = saksbehandlerIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL, enhet = "5678"),
                SignaturGrunnlag(navIdent = kvalitetssikrerIdent, rolle = Rolle.KVALITETSSIKRER, enhet = "3456"),
                SignaturGrunnlag(navIdent = veilederIdent, rolle = Rolle.SAKSBEHANDLER_OPPFOLGING, enhet = "2345")
            )
        } else {
            assertThat(signaturer).containsExactly( // NB: Tester også rekkefølge.
                SignaturGrunnlag(navIdent = beslutterIdent, rolle = Rolle.BESLUTTER, enhet = null),
                SignaturGrunnlag(navIdent = saksbehandlerIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL, enhet = null),
                SignaturGrunnlag(navIdent = kvalitetssikrerIdent, rolle = Rolle.KVALITETSSIKRER, enhet = null),
                SignaturGrunnlag(navIdent = veilederIdent, rolle = Rolle.SAKSBEHANDLER_OPPFOLGING, enhet = null)
            )
        }
    }

    @Test
    fun `skal fjerne duplikater om samme person har løst avklaringsbehov som er knytte til ulike roller – eksempel samme person på lokalkontor og NAY`() {
        val behandling = gittBehandling()
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
        if (hentEnhetFraOppgave) { // Identisk assert i tillegg til å sjekke enhet
            assertThat(signaturer).containsExactlyInAnyOrder(
                SignaturGrunnlag(navIdent = beslutterIdent, rolle = Rolle.BESLUTTER, enhet = "6789"),
                SignaturGrunnlag(navIdent = kvalitetssikrerIdent, rolle = Rolle.KVALITETSSIKRER, enhet = "5678"),
                SignaturGrunnlag(navIdent = veilederIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL, enhet = "4567")
            )
        } else {
            assertThat(signaturer).containsExactlyInAnyOrder(
                SignaturGrunnlag(navIdent = beslutterIdent, rolle = Rolle.BESLUTTER, enhet = null),
                SignaturGrunnlag(navIdent = kvalitetssikrerIdent, rolle = Rolle.KVALITETSSIKRER, enhet = null),
                SignaturGrunnlag(navIdent = veilederIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL, enhet = null)
            )
        }
    }

    @Test
    fun `tar med innlogget bruker i signatur for vedtaksbrev dersom beslutter ikke har saksbehandlet`() {
        val behandling = gittBehandling()
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

        if (hentEnhetFraOppgave) {
            assertThat(signaturer).containsExactlyInAnyOrder(
                SignaturGrunnlag(navIdent = innloggetBrukerIdent, rolle = Rolle.BESLUTTER, enhet = "2345"),
                SignaturGrunnlag(navIdent = saksbehandlerIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL, enhet = "1234")
            )
        } else {
            assertThat(signaturer).containsExactlyInAnyOrder(
                SignaturGrunnlag(navIdent = innloggetBrukerIdent, rolle = null, enhet = null),
                SignaturGrunnlag(navIdent = saksbehandlerIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL, enhet = null)
            )
        }
    }

    @Test
    fun `dersom beslutter ikke har saksbehandlet og innlogget bruker har saksbehandlet beholdes rollen til innlogget bruker`() {
        val behandling = gittBehandling()
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
        if (hentEnhetFraOppgave) { // Identisk assert i tillegg til å sjekke enhet
            assertThat(signaturer).containsExactly(
                SignaturGrunnlag(navIdent = saksbehandlerIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL, enhet = "1234"),
                SignaturGrunnlag(navIdent = kvalitetssikrerIdent, rolle = Rolle.KVALITETSSIKRER, enhet = "3456"),
                SignaturGrunnlag(navIdent = veilederIdent, rolle = Rolle.SAKSBEHANDLER_OPPFOLGING, enhet = "2345"),
            )
        } else {
            assertThat(signaturer).containsExactly(
                SignaturGrunnlag(navIdent = saksbehandlerIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL, enhet = null),
                SignaturGrunnlag(navIdent = kvalitetssikrerIdent, rolle = Rolle.KVALITETSSIKRER, enhet = null),
                SignaturGrunnlag(navIdent = veilederIdent, rolle = Rolle.SAKSBEHANDLER_OPPFOLGING, enhet = null),
            )
        }
    }

    private fun leggTilEndring(
        behandling: Behandling,
        definisjon: Definisjon,
        endretAv: String,
        status: AvklaringsbehovStatus,
        oppgaveEnhet: String? = null,
    ) {
        val avklaringsbehovene = behandlingTilAvklaringsbehovene[behandling.id]?.toMutableList() ?: mutableListOf()
        val avklaringsbehov = avklaringsbehovene.find { it.definisjon == definisjon } ?: Avklaringsbehov(
            -1, definisjon, mutableListOf(), StegType.UDEFINERT, false
        ).also {
            avklaringsbehovene.add(it)
        }
        behandlingTilAvklaringsbehovene.set(behandling.id, avklaringsbehovene)
        val tidsstempel =
            avklaringsbehovene.flatMap { it.historikk }.maxOrNull()?.tidsstempel?.plusMinutes(1) ?: LocalDateTime.now()

        avklaringsbehov.historikk.add(
            Endring(
                status = status,
                tidsstempel = tidsstempel,
                begrunnelse = "",
                endretAv = endretAv
            )
        )

        if (oppgaveEnhet != null) {
            val eksistende = behandlingTilOppgaveEnhet[behandling.referanse] ?: emptyList()
            behandlingTilOppgaveEnhet[behandling.referanse] =
                eksistende
                    .filterNot { it.avklaringsbehovKode == definisjon.kode.name }
                    .plus(OppgaveEnhetDto(definisjon.kode.name, oppgaveEnhet))
        }
    }

    fun gittBehandling(): Behandling {
        val behandlingId = BehandlingId(Random.nextLong())
        val behandling = Behandling(
            id = behandlingId,
            forrigeBehandlingId = null,
            sakId = SakId(Random.nextLong()),
            status = no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.IVERKSETTES,
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            årsakTilOpprettelse = null,
            versjon = 1,
        )

        every { behandlingRepository.hent(behandlingId) } returns behandling

        return behandling
    }
}
