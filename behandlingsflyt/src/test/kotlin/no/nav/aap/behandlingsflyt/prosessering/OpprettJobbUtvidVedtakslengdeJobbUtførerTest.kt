package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.Runs
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime

@ExtendWith(MockKExtension::class)
class OpprettJobbUtvidVedtakslengdeJobbUtførerTest {

    private val sakId = SakId(1L)
    private val behandlingId = BehandlingId(1L)

    val underveisRepository = mockk<UnderveisRepository>()
    val sakOgBehandlingService = mockk<SakOgBehandlingService>()
    val flytJobbRepository = mockk<FlytJobbRepository>()
    val unleashGateway = mockk<UnleashGateway> {
        every { isEnabled(BehandlingsflytFeature.UtvidVedtakslengdeJobb) } returns true
    }
    val opprettJobbUtvidVedtakslengdeJobbUtfører =
        OpprettJobbUtvidVedtakslengdeJobbUtfører(underveisRepository, sakOgBehandlingService, flytJobbRepository, unleashGateway)
    val jobbInput = JobbInput(OpprettJobbUtvidVedtakslengdeJobbUtfører)


    // TODO kan fjernes når vi ikke lenger har miljøspesifikke filter i OpprettJobbUtvidVedtakslengdeJobbUtførerTest
    @BeforeEach
    fun setup() {
        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
    }

    @Test
    fun `skal opprette jobber for behandlinger hvor vedtakslengde skal forlenges`() {
        val jobbInputSak = JobbInput(OpprettBehandlingUtvidVedtakslengdeJobbUtfører).forSak(sakId.id)

        every { underveisRepository.hentSakerMedSisteUnderveisperiodeFørDato(any()) } returns setOf(sakId)
        every { sakOgBehandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns behandling()
        every { flytJobbRepository.leggTil(any()) } just Runs

        opprettJobbUtvidVedtakslengdeJobbUtfører.utfør(jobbInput)

        val slot = slot<JobbInput>()
        verify(exactly = 1) { flytJobbRepository.leggTil(capture(slot)) }

        assertThat(slot.captured)
            .usingRecursiveComparison()
            .isEqualTo(jobbInputSak)
    }

    private fun behandling(status: Status = Status.IVERKSETTES) =
        Behandling(
            sakId = sakId,
            id = behandlingId,
            referanse = BehandlingReferanse(),
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            status = status,
            opprettetTidspunkt = LocalDateTime.now(),
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
            forrigeBehandlingId = null,
            versjon = 1L
        )
}