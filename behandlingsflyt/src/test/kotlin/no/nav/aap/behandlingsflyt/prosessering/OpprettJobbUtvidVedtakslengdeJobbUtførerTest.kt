package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.Runs
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.LocalDateTime

object VedtakslengdeUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(
        BehandlingsflytFeature.UtvidVedtakslengdeJobb,
    )
)

@ExtendWith(MockKExtension::class)
class OpprettJobbUtvidVedtakslengdeJobbUtførerTest {

    private val dagensDato = 1 desember 2025
    private val clock = fixedClock(dagensDato)
    private val sakId = SakId(1L)
    private val behandlingId = BehandlingId(1L)
    private val jobbInput = JobbInput(OpprettJobbUtvidVedtakslengdeJobbUtfører)

    private val sakOgBehandlingService = mockk<SakOgBehandlingService>()
    private val vedtakslengdeService = mockk<VedtakslengdeService>()
    private val flytJobbRepository = mockk<FlytJobbRepository>()
    private val opprettJobbUtvidVedtakslengdeJobbUtfører =
        OpprettJobbUtvidVedtakslengdeJobbUtfører(
            sakOgBehandlingService = sakOgBehandlingService,
            vedtakslengdeService = vedtakslengdeService,
            flytJobbRepository = flytJobbRepository,
            unleashGateway = VedtakslengdeUnleash,
            clock = clock
        )

    // TODO kan fjernes når vi ikke lenger har miljøspesifikke filter i OpprettJobbUtvidVedtakslengdeJobbUtfører
    @BeforeEach
    fun setup() {
        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
    }

    @Test
    fun `skal opprette jobber for behandlinger hvor vedtakslengde skal forlenges`() {
        val jobbInputSak = JobbInput(OpprettBehandlingUtvidVedtakslengdeJobbUtfører).forSak(sakId.id)

        every { vedtakslengdeService.hentSakerAktuelleForUtvidelseAvVedtakslengde(any()) } returns setOf(sakId)
        every { vedtakslengdeService.skalUtvideSluttdato(behandlingId, behandlingId, any<LocalDate>())} returns true
        every { sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns behandlingMedVedtak()
        every { flytJobbRepository.leggTil(match { it.sakId() == sakId.id }) } just Runs

        opprettJobbUtvidVedtakslengdeJobbUtfører.utfør(jobbInput)

        val slot = slot<JobbInput>()
        verify(exactly = 1) { flytJobbRepository.leggTil(capture(slot)) }

        assertThat(slot.captured)
            .usingRecursiveComparison()
            .isEqualTo(jobbInputSak)
    }

    @Test
    fun `skal ikke opprette jobber hvis ingen saker returneres`() {
        every { vedtakslengdeService.hentSakerAktuelleForUtvidelseAvVedtakslengde(any()) } returns emptySet()

        opprettJobbUtvidVedtakslengdeJobbUtfører.utfør(jobbInput)

        verify(exactly = 0) { flytJobbRepository.leggTil(any()) }
    }

    private fun behandlingMedVedtak(): BehandlingMedVedtak =
        BehandlingMedVedtak(
            saksnummer = Saksnummer("123"),
            id = behandlingId,
            referanse = BehandlingReferanse(),
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            status = Status.IVERKSETTES,
            opprettetTidspunkt = LocalDateTime.now(clock),
            vedtakstidspunkt = LocalDateTime.now(clock),
            virkningstidspunkt = null,
            vurderingsbehov = setOf(),
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD
        )

}