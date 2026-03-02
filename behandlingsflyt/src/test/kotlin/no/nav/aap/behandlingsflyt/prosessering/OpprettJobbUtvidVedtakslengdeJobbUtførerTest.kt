package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeService
import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeUtvidelse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OpprettJobbUtvidVedtakslengdeJobbUtførerTest {

    private val dagensDato = 1 desember 2025
    private val clock = fixedClock(dagensDato)
    private val sakId = SakId(1L)
    private val behandlingId = BehandlingId(1L)
    private val jobbInput = JobbInput(OpprettJobbUtvidVedtakslengdeJobbUtfører)

    private val behandlingService = mockk<BehandlingService>()
    private val vedtakslengdeService = mockk<VedtakslengdeService>()
    private val sakRepository = mockk<SakRepository>()
    private val flytJobbRepository = mockk<FlytJobbRepository>()
    private val opprettJobbUtvidVedtakslengdeJobbUtfører =
        OpprettJobbUtvidVedtakslengdeJobbUtfører(
            behandlingService = behandlingService,
            vedtakslengdeService = vedtakslengdeService,
            sakRepository = sakRepository,
            flytJobbRepository = flytJobbRepository,
            clock = clock
        )

    @Test
    fun `skal opprette jobber for behandlinger hvor vedtakslengde skal forlenges`() {
        val jobbInputSak = JobbInput(OpprettBehandlingUtvidVedtakslengdeJobbUtfører).forSak(sakId.id)

        every { vedtakslengdeService.hentSakerAktuelleForUtvidelseAvVedtakslengde(any()) } returns setOf(sakId)
        every { vedtakslengdeService.hentNesteVedtakslengdeUtvidelse(behandlingId, behandlingId, any<Periode>())} returns VedtakslengdeUtvidelse.Automatisk(
            forrigeSluttdato = dagensDato,
            nySluttdato = dagensDato.plusYears(1),
        )
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns behandlingMedVedtak()
        every { sakRepository.hent(sakId) } returns mockk<Sak> { every { rettighetsperiode } returns Periode(dagensDato.minusYears(1), dagensDato) }
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

    @Test
    fun `skal ikke opprette jobber hvis hentNesteVedtakslengdeUtvidelse gir IngenFramtidigOrdinærRettighet`() {
        every { vedtakslengdeService.hentSakerAktuelleForUtvidelseAvVedtakslengde(any()) } returns setOf(sakId)
        every { vedtakslengdeService.hentNesteVedtakslengdeUtvidelse(behandlingId, behandlingId, any<Periode>())} returns VedtakslengdeUtvidelse.IngenFremtidigOrdinærRettighet
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns behandlingMedVedtak()
        every { sakRepository.hent(sakId) } returns mockk<Sak> { every { rettighetsperiode } returns Periode(dagensDato.minusYears(1), dagensDato) }

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