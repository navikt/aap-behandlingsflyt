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
class SignaturServiceTest(val hentEnhetFraOppgave: Boolean) {


    private val unleashGateway = mockk<UnleashGateway>()
    private val oppgavestyringGateway = mockk<OppgavestyringGateway>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val avklaringsbehovRepository = mockk<AvklaringsbehovRepository>()
    private val avklaringsbehovOperasjonerRepository = mockk<AvklaringsbehovOperasjonerRepository>()
    private val signaturService =
        SignaturService(unleashGateway, oppgavestyringGateway, behandlingRepository, avklaringsbehovRepository)

    private val behandlingTilAvklaringsbehovene = mutableMapOf<BehandlingId, List<Avklaringsbehov>>()

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
    }

    @Test
    fun `den som står i signatur for en gitt rolle er den som utførte siste avklaringsbehovet for rollen (status AVSLUTTET)`() {
        val behandling = gittBehandling()
        every { oppgavestyringGateway.hentOppgaveEnhet(behandling.referanse) } returns OppgaveEnhetResponse(listOf())
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
            behandling.id,
            Definisjon.FRITAK_MELDEPLIKT,
            endretAv = veilederIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )
        leggTilEndring(
            behandling.id,
            Definisjon.AVKLAR_SYKDOM,
            endretAv = SYSTEMBRUKER.ident,
            AvklaringsbehovStatus.OPPRETTET
        )
        leggTilEndring(
            behandling.id,
            Definisjon.AVKLAR_SYKDOM,
            endretAv = veilederIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )
        leggTilEndring(
            behandling.id,
            Definisjon.AVKLAR_SYKDOM,
            endretAv = kvalitetssikrerIdent,
            AvklaringsbehovStatus.KVALITETSSIKRET
        )
        leggTilEndring(
            behandling.id,
            Definisjon.AVKLAR_SYKDOM,
            endretAv = beslutterIdent,
            AvklaringsbehovStatus.TOTRINNS_VURDERT
        )

        // KVALITETSSIKRER
        leggTilEndring(
            behandling.id,
            Definisjon.KVALITETSSIKRING,
            endretAv = kvalitetssikrerIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )

        // SAKSBEHANDLER_NASJONAL
        leggTilEndring(
            behandling.id,
            Definisjon.AVKLAR_STUDENT,
            endretAv = saksbehandlerIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )
        leggTilEndring(
            behandling.id,
            Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT,
            endretAv = saksbehandlerIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )

        // definisjon som ikke skal tas høyde for
        leggTilEndring(
            behandling.id,
            Definisjon.MANUELT_SATT_PÅ_VENT,
            endretAv = saksbehandlerIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )

        // definisjon som ikke skal tas høyde for
        leggTilEndring(
            behandling.id,
            Definisjon.VURDER_TREKK_AV_SØKNAD,
            endretAv = saksbehandlerIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )

        // BESLUTTER
        leggTilEndring(
            behandling.id,
            Definisjon.FATTE_VEDTAK,
            endretAv = beslutterIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )

        val signaturer = signaturService.finnSignaturGrunnlag(brevbestilling, Bruker(""))

        assertThat(signaturer).containsExactly( // NB: Tester også rekkefølge.
            SignaturGrunnlag(navIdent = beslutterIdent, rolle = Rolle.BESLUTTER),
            SignaturGrunnlag(navIdent = saksbehandlerIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL),
            SignaturGrunnlag(navIdent = kvalitetssikrerIdent, rolle = Rolle.KVALITETSSIKRER),
            SignaturGrunnlag(navIdent = veilederIdent, rolle = Rolle.SAKSBEHANDLER_OPPFOLGING)
        )
    }

    @Test
    fun `skal fjerne duplikater om samme person har løst avklaringsbehov som er knytte til ulike roller – eksempel samme person på lokalkontor og NAY`() {
        val behandling = gittBehandling()
        every { oppgavestyringGateway.hentOppgaveEnhet(behandling.referanse) } returns OppgaveEnhetResponse(listOf())
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
            behandling.id,
            Definisjon.FRITAK_MELDEPLIKT,
            endretAv = veilederIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )
        leggTilEndring(
            behandling.id,
            Definisjon.AVKLAR_SYKDOM,
            endretAv = SYSTEMBRUKER.ident,
            AvklaringsbehovStatus.OPPRETTET
        )
        leggTilEndring(
            behandling.id,
            Definisjon.AVKLAR_SYKDOM,
            endretAv = veilederIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )
        leggTilEndring(
            behandling.id,
            Definisjon.AVKLAR_SYKDOM,
            endretAv = kvalitetssikrerIdent,
            AvklaringsbehovStatus.KVALITETSSIKRET
        )
        leggTilEndring(
            behandling.id,
            Definisjon.AVKLAR_SYKDOM,
            endretAv = beslutterIdent,
            AvklaringsbehovStatus.TOTRINNS_VURDERT
        )

        // SAKSBEHANDLER_NASJONAL
        leggTilEndring(
            behandling.id,
            Definisjon.AVKLAR_STUDENT,
            endretAv = veilederIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )
        leggTilEndring(
            behandling.id,
            Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT,
            endretAv = veilederIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )

        // KVALITETSSIKRER
        leggTilEndring(
            behandling.id,
            Definisjon.KVALITETSSIKRING,
            endretAv = kvalitetssikrerIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )

        // BESLUTTER
        leggTilEndring(
            behandling.id,
            Definisjon.FATTE_VEDTAK,
            endretAv = beslutterIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )

        val signaturer = signaturService.finnSignaturGrunnlag(brevbestilling, Bruker(""))

        assertThat(signaturer).hasSize(3)
        assertThat(signaturer).containsExactlyInAnyOrder(
            SignaturGrunnlag(navIdent = beslutterIdent, rolle = Rolle.BESLUTTER),
            SignaturGrunnlag(navIdent = kvalitetssikrerIdent, rolle = Rolle.KVALITETSSIKRER),
            SignaturGrunnlag(navIdent = veilederIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL)
        )
    }

    @Test
    fun `tar med innlogget bruker i signatur for vedtaksbrev dersom beslutter ikke har saksbehandlet`() {
        val behandling = gittBehandling()
        every { oppgavestyringGateway.hentOppgaveEnhet(behandling.referanse) } returns OppgaveEnhetResponse(listOf())
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
            behandling.id,
            Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT,
            endretAv = saksbehandlerIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )

        // BESLUTTER
        leggTilEndring(
            behandling.id,
            Definisjon.SKRIV_VEDTAKSBREV,
            endretAv = SYSTEMBRUKER.ident,
            AvklaringsbehovStatus.OPPRETTET
        )

        val signaturer = signaturService.finnSignaturGrunnlag(brevbestilling, Bruker(innloggetBrukerIdent))

        if (hentEnhetFraOppgave) {
            assertThat(signaturer).containsExactlyInAnyOrder(
                SignaturGrunnlag(navIdent = innloggetBrukerIdent, rolle = Rolle.BESLUTTER),
                SignaturGrunnlag(navIdent = saksbehandlerIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL)
            )
        } else {
            assertThat(signaturer).containsExactlyInAnyOrder(
                SignaturGrunnlag(navIdent = innloggetBrukerIdent, rolle = null),
                SignaturGrunnlag(navIdent = saksbehandlerIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL)
            )
        }
    }

    @Test
    fun `dersom beslutter ikke har saksbehandlet og innlogget bruker har saksbehandlet beholdes rollen til innlogget bruker`() {
        val behandling = gittBehandling()
        every { oppgavestyringGateway.hentOppgaveEnhet(behandling.referanse) } returns OppgaveEnhetResponse(listOf())
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
            behandling.id,
            Definisjon.FASTSETT_PÅKLAGET_BEHANDLING,
            endretAv = saksbehandlerIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )
        leggTilEndring(
            behandling.id,
            Definisjon.FASTSETT_FULLMEKTIG,
            endretAv = saksbehandlerIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )
        leggTilEndring(
            behandling.id,
            Definisjon.VURDER_FORMKRAV,
            endretAv = saksbehandlerIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )
        leggTilEndring(
            behandling.id,
            Definisjon.FASTSETT_BEHANDLENDE_ENHET,
            endretAv = saksbehandlerIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )
        leggTilEndring(
            behandling.id,
            Definisjon.VURDER_KLAGE_KONTOR,
            endretAv = veilederIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )
        leggTilEndring(
            behandling.id,
            Definisjon.KVALITETSSIKRING,
            endretAv = kvalitetssikrerIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )
        leggTilEndring(
            behandling.id,
            Definisjon.VURDER_KLAGE_KONTOR,
            endretAv = veilederIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )
        leggTilEndring(
            behandling.id,
            Definisjon.KVALITETSSIKRING,
            endretAv = kvalitetssikrerIdent,
            AvklaringsbehovStatus.AVSLUTTET
        )

        val signaturer = signaturService.finnSignaturGrunnlag(brevbestilling, Bruker(kvalitetssikrerIdent))

        assertThat(signaturer).containsExactly(
            SignaturGrunnlag(navIdent = saksbehandlerIdent, rolle = Rolle.SAKSBEHANDLER_NASJONAL),
            SignaturGrunnlag(navIdent = kvalitetssikrerIdent, rolle = Rolle.KVALITETSSIKRER),
            SignaturGrunnlag(navIdent = veilederIdent, rolle = Rolle.SAKSBEHANDLER_OPPFOLGING),
        )
    }

    private val avklaringsbehovene = mutableListOf<Avklaringsbehov>()

    private fun leggTilEndring(
        behandlingId: BehandlingId,
        definisjon: Definisjon,
        endretAv: String,
        status: AvklaringsbehovStatus
    ) {
        val avklaringsbehovene = behandlingTilAvklaringsbehovene[behandlingId]?.toMutableList() ?: mutableListOf()
        val avklaringsbehov = avklaringsbehovene.find { it.definisjon == definisjon } ?: Avklaringsbehov(
            -1, definisjon, mutableListOf(), StegType.UDEFINERT, false
        ).also {
            avklaringsbehovene.add(it)
        }
        behandlingTilAvklaringsbehovene.set(behandlingId, avklaringsbehovene)
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
