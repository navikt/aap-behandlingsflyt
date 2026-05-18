package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.gregulering.GReguleringService
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakId
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.motor.JobbInput
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OpprettBehandlingGReguleringJobbUtførerTest {

    private val dagensDato = 1 desember 2025
    private val clock = fixedClock(dagensDato)
    private val sakId = SakId(1L)
    private val behandlingId = BehandlingId(1L)
    private val jobbInput = JobbInput(OpprettBehandlingGReguleringJobbUtfører).forSak(sakId.id)

    private val prosesserBehandlingService = mockk<ProsesserBehandlingService>()
    private val behandlingService = mockk<BehandlingService>()
    private val gReguleringService = mockk<GReguleringService>()
    private val unleashGateway = mockk<UnleashGateway>()

    private fun opprettUtfører() =
        OpprettBehandlingGReguleringJobbUtfører(
            prosesserBehandlingService = prosesserBehandlingService,
            behandlingService = behandlingService,
            gReguleringService = gReguleringService,
            unleashGateway = unleashGateway,
        )

    private fun enableToggle() {
        every { unleashGateway.isDisabled(BehandlingsflytFeature.GReguleringsJobb) } returns false
    }

    private fun disableToggle() {
        every { unleashGateway.isDisabled(BehandlingsflytFeature.GReguleringsJobb) } returns true
    }

    @Test
    fun `skal opprette behandling når grunnbeløpet er endret for gjeldende behandling`() {
        enableToggle()
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns null
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns behandlingMedVedtak()
        every { gReguleringService.erGrunnbeløpEndretForBehandling(behandlingId) } returns true
        every {
            behandlingService.finnEllerOpprettBehandling(
                sakId = sakId,
                vurderingsbehovOgÅrsak = match { vurderingsbehovOgÅrsak ->
                    vurderingsbehovOgÅrsak.årsak == ÅrsakTilOpprettelse.G_REGULERING &&
                        vurderingsbehovOgÅrsak.vurderingsbehov.single().type == Vurderingsbehov.G_REGULERING
                }
            )
        } returns opprettetBehandling()
        every { prosesserBehandlingService.triggProsesserBehandling(any<BehandlingService.OpprettetBehandling>()) } just Runs

        opprettUtfører().utfør(jobbInput)

        verify(exactly = 1) { prosesserBehandlingService.triggProsesserBehandling(any<BehandlingService.OpprettetBehandling>()) }
        verify(exactly = 1) {
            behandlingService.finnEllerOpprettBehandling(
                sakId = sakId,
                vurderingsbehovOgÅrsak = match { vurderingsbehovOgÅrsak ->
                    vurderingsbehovOgÅrsak.årsak == ÅrsakTilOpprettelse.G_REGULERING &&
                        vurderingsbehovOgÅrsak.vurderingsbehov.single().type == Vurderingsbehov.G_REGULERING
                }
            )
        }
    }

    @Test
    fun `skal ikke opprette behandling når grunnbeløpet ikke er endret for gjeldende behandling`() {
        enableToggle()
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns null
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns behandlingMedVedtak()
        every { gReguleringService.erGrunnbeløpEndretForBehandling(behandlingId) } returns false

        opprettUtfører().utfør(jobbInput)

        verify(exactly = 0) { prosesserBehandlingService.triggProsesserBehandling(any<BehandlingService.OpprettetBehandling>()) }
        verify(exactly = 0) { behandlingService.finnEllerOpprettBehandling(any<SakId>(), any<VurderingsbehovOgÅrsak>()) }
    }

    @Test
    fun `skal ikke opprette behandling når det ikke finnes gjeldende behandling`() {
        enableToggle()
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns null
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns null

        opprettUtfører().utfør(jobbInput)

        verify(exactly = 0) { prosesserBehandlingService.triggProsesserBehandling(any<BehandlingService.OpprettetBehandling>()) }
        verify(exactly = 0) { behandlingService.finnEllerOpprettBehandling(any<SakId>(), any<VurderingsbehovOgÅrsak>()) }
    }

    @Test
    fun `skal ikke opprette behandling når feature toggle er avskrudd`() {
        disableToggle()

        opprettUtfører().utfør(jobbInput)

        verify(exactly = 0) { behandlingService.finnSisteYtelsesbehandlingFor(any()) }
        verify(exactly = 0) { behandlingService.finnBehandlingMedSisteFattedeVedtak(any()) }
        verify(exactly = 0) { prosesserBehandlingService.triggProsesserBehandling(any<BehandlingService.OpprettetBehandling>()) }
    }

    @Test
    fun `skal ikke opprette behandling når saken har en åpen førstegangsbehandling`() {
        enableToggle()
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns behandling(
            status = Status.OPPRETTET,
            typeBehandling = TypeBehandling.Førstegangsbehandling,
        )

        opprettUtfører().utfør(jobbInput)

        verify(exactly = 0) { prosesserBehandlingService.triggProsesserBehandling(any<BehandlingService.OpprettetBehandling>()) }
        verify(exactly = 0) { behandlingService.finnEllerOpprettBehandling(any<SakId>(), any<VurderingsbehovOgÅrsak>()) }
        verify(exactly = 0) { behandlingService.finnBehandlingMedSisteFattedeVedtak(any()) }
    }

    @Test
    fun `skal opprette behandling når saken har en åpen revurdering og grunnbeløpet er endret`() {
        enableToggle()
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns behandling(
            status = Status.UTREDES,
            typeBehandling = TypeBehandling.Revurdering,
        )
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns behandlingMedVedtak()
        every { gReguleringService.erGrunnbeløpEndretForBehandling(behandlingId) } returns true
        every {
            behandlingService.finnEllerOpprettBehandling(
                sakId = sakId,
                vurderingsbehovOgÅrsak = match { vurderingsbehovOgÅrsak ->
                    vurderingsbehovOgÅrsak.årsak == ÅrsakTilOpprettelse.G_REGULERING &&
                        vurderingsbehovOgÅrsak.vurderingsbehov.single().type == Vurderingsbehov.G_REGULERING
                }
            )
        } returns opprettetBehandling()
        every { prosesserBehandlingService.triggProsesserBehandling(any<BehandlingService.OpprettetBehandling>()) } just Runs

        opprettUtfører().utfør(jobbInput)

        verify(exactly = 1) { prosesserBehandlingService.triggProsesserBehandling(any<BehandlingService.OpprettetBehandling>()) }
    }

    private fun behandling(
        status: Status = Status.IVERKSETTES,
        vurderingsbehov: List<VurderingsbehovMedPeriode> = emptyList(),
        typeBehandling: TypeBehandling = TypeBehandling.Førstegangsbehandling,
    ) =
        Behandling(
            sakId = sakId,
            id = behandlingId,
            referanse = BehandlingReferanse(),
            typeBehandling = typeBehandling,
            status = status,
            vurderingsbehov = vurderingsbehov,
            opprettetTidspunkt = LocalDateTime.now(clock),
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
            forrigeBehandlingId = null,
            versjon = 1L
        )

    private fun behandlingMedVedtak(): BehandlingMedVedtak =
        BehandlingMedVedtak(
            saksnummer = Saksnummer("123"),
            id = behandlingId,
            referanse = BehandlingReferanse(),
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            status = Status.IVERKSETTES,
            opprettetTidspunkt = LocalDateTime.now(clock),
            vedtakId = VedtakId(0),
            vedtakstidspunkt = LocalDateTime.now(clock),
            virkningstidspunkt = null,
            vurderingsbehov = setOf(),
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
            forrigeBehandlingId = null,
        )

    private fun opprettetBehandling() =
        BehandlingService.MåBehandlesAtomært(
            nyBehandling = behandling(),
            åpenBehandling = null
        )
}
