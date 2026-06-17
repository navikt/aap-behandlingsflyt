package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakId
import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeService
import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeUtvidelse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.fixedClock
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
    private val flytJobbRepository = mockk<FlytJobbRepository>()

    private fun opprettJobbUtfører() =
        OpprettJobbUtvidVedtakslengdeJobbUtfører(
            behandlingService = behandlingService,
            vedtakslengdeService = vedtakslengdeService,
            flytJobbRepository = flytJobbRepository,
            clock = clock
        )

    @Test
    fun `skal opprette jobber for behandlinger hvor vedtakslengde skal forlenges`() {
        val jobbInputSak = JobbInput(OpprettBehandlingUtvidVedtakslengdeJobbUtfører).forSak(sakId.id)

        every { vedtakslengdeService.hentSakerAktuelleForUtvidelseAvVedtakslengde(any()) } returns setOf(sakId)
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns null
        every { vedtakslengdeService.hentNesteVedtakslengdeUtvidelse(behandlingId, behandlingId)} returns VedtakslengdeUtvidelse.Automatisk(
            forrigeSluttdato = dagensDato,
            nySluttdato = dagensDato.plusYears(1),
        )
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns behandlingMedVedtak()
        every { flytJobbRepository.hentJobberForSak(sakId.toLong()) } returns emptyList()
        every { flytJobbRepository.leggTil(match { it.sakId() == sakId.id }) } just Runs

        opprettJobbUtfører().utfør(jobbInput)

        val slot = slot<JobbInput>()
        verify(exactly = 1) { flytJobbRepository.leggTil(capture(slot)) }

        assertThat(slot.captured)
            .usingRecursiveComparison()
            .isEqualTo(jobbInputSak)
    }

    @Test
    fun `skal ikke opprette jobber hvis ingen saker returneres`() {
        every { vedtakslengdeService.hentSakerAktuelleForUtvidelseAvVedtakslengde(any()) } returns emptySet()

        opprettJobbUtfører().utfør(jobbInput)

        verify(exactly = 0) { flytJobbRepository.leggTil(any()) }
    }

    @Test
    fun `skal ikke opprette jobber hvis hentNesteVedtakslengdeUtvidelse gir IngenFramtidigOrdinærRettighet`() {
        every { vedtakslengdeService.hentSakerAktuelleForUtvidelseAvVedtakslengde(any()) } returns setOf(sakId)
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns null
        every { vedtakslengdeService.hentNesteVedtakslengdeUtvidelse(behandlingId, behandlingId)} returns VedtakslengdeUtvidelse.IngenFremtidigBistandsbehovRettighet
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns behandlingMedVedtak()

        opprettJobbUtfører().utfør(jobbInput)

        verify(exactly = 0) { flytJobbRepository.leggTil(any()) }
    }

    @Test
    fun `skal opprette jobb hvis hentNesteVedtakslengdeUtvidelse gir Manuell`() {
        val jobbInputSak = JobbInput(OpprettBehandlingUtvidVedtakslengdeJobbUtfører).forSak(sakId.id)

        every { vedtakslengdeService.hentSakerAktuelleForUtvidelseAvVedtakslengde(any()) } returns setOf(sakId)
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns null
        every { vedtakslengdeService.hentNesteVedtakslengdeUtvidelse(behandlingId, behandlingId) } returns VedtakslengdeUtvidelse.Manuell(
            forrigeSluttdato = dagensDato,
        )
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns behandlingMedVedtak()
        every { flytJobbRepository.hentJobberForSak(sakId.toLong()) } returns emptyList()
        every { flytJobbRepository.leggTil(match { it.sakId() == sakId.id }) } just Runs

        opprettJobbUtfører().utfør(jobbInput)

        val slot = slot<JobbInput>()
        verify(exactly = 1) { flytJobbRepository.leggTil(capture(slot)) }

        assertThat(slot.captured)
            .usingRecursiveComparison()
            .isEqualTo(jobbInputSak)
    }

    @Test
    fun `skal ikke opprette jobber hvis sak ikke har gjeldende vedtatt behandling`() {
        every { vedtakslengdeService.hentSakerAktuelleForUtvidelseAvVedtakslengde(any()) } returns setOf(sakId)
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns null
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns null

        opprettJobbUtfører().utfør(jobbInput)

        verify(exactly = 0) { flytJobbRepository.leggTil(any()) }
    }

    @Test
    fun `skal ikke inkludere kandidat som allerede har åpen behandling med årsak UTVID_VEDTAKSLENGDE`() {
        val åpenBehandling = Behandling(
            id = BehandlingId(2L),
            forrigeBehandlingId = behandlingId,
            sakId = sakId,
            typeBehandling = TypeBehandling.Revurdering,
            status = Status.OPPRETTET,
            årsakTilOpprettelse = ÅrsakTilOpprettelse.UTVID_VEDTAKSLENGDE,
            versjon = 0L
        )

        every { vedtakslengdeService.hentSakerAktuelleForUtvidelseAvVedtakslengde(any()) } returns setOf(sakId)
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns åpenBehandling

        opprettJobbUtfører().utfør(jobbInput)

        verify(exactly = 0) { flytJobbRepository.leggTil(any()) }
    }

    @Test
    fun `skal inkludere kandidat som har avsluttet behandling med årsak UTVID_VEDTAKSLENGDE`() {
        val jobbInputSak = JobbInput(OpprettBehandlingUtvidVedtakslengdeJobbUtfører).forSak(sakId.id)
        val avsluttetBehandling = Behandling(
            id = BehandlingId(2L),
            forrigeBehandlingId = behandlingId,
            sakId = sakId,
            typeBehandling = TypeBehandling.Revurdering,
            status = Status.AVSLUTTET,
            årsakTilOpprettelse = ÅrsakTilOpprettelse.UTVID_VEDTAKSLENGDE,
            versjon = 0L
        )

        every { vedtakslengdeService.hentSakerAktuelleForUtvidelseAvVedtakslengde(any()) } returns setOf(sakId)
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns avsluttetBehandling
        every { vedtakslengdeService.hentNesteVedtakslengdeUtvidelse(behandlingId, behandlingId) } returns VedtakslengdeUtvidelse.Automatisk(
            forrigeSluttdato = dagensDato,
            nySluttdato = dagensDato.plusYears(1),
        )
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns behandlingMedVedtak()
        every { flytJobbRepository.hentJobberForSak(sakId.toLong()) } returns emptyList()
        every { flytJobbRepository.leggTil(match { it.sakId() == sakId.id }) } just Runs

        opprettJobbUtfører().utfør(jobbInput)

        val slot = slot<JobbInput>()
        verify(exactly = 1) { flytJobbRepository.leggTil(capture(slot)) }

        assertThat(slot.captured)
            .usingRecursiveComparison()
            .isEqualTo(jobbInputSak)
    }

    @Test
    fun `skal ikke opprette jobb for sak som allerede har ventende utvidVedtakslengde-jobb`() {
        val eksisterendeJobb = JobbInput(OpprettBehandlingUtvidVedtakslengdeJobbUtfører).forSak(sakId.id)

        every { vedtakslengdeService.hentSakerAktuelleForUtvidelseAvVedtakslengde(any()) } returns setOf(sakId)
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns null
        every { vedtakslengdeService.hentNesteVedtakslengdeUtvidelse(behandlingId, behandlingId) } returns VedtakslengdeUtvidelse.Automatisk(
            forrigeSluttdato = dagensDato,
            nySluttdato = dagensDato.plusYears(1),
        )
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns behandlingMedVedtak()
        every { flytJobbRepository.hentJobberForSak(sakId.toLong()) } returns listOf(eksisterendeJobb)

        opprettJobbUtfører().utfør(jobbInput)

        verify(exactly = 0) { flytJobbRepository.leggTil(any()) }
    }

    @Test
    fun `skal opprette jobb for sak som har andre ventende jobber men ikke utvidVedtakslengde-jobb`() {
        val jobbInputSak = JobbInput(OpprettBehandlingUtvidVedtakslengdeJobbUtfører).forSak(sakId.id)
        val annenJobb = JobbInput(OpprettBehandlingFritakMeldepliktJobbUtfører).forSak(sakId.id)

        every { vedtakslengdeService.hentSakerAktuelleForUtvidelseAvVedtakslengde(any()) } returns setOf(sakId)
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns null
        every { vedtakslengdeService.hentNesteVedtakslengdeUtvidelse(behandlingId, behandlingId) } returns VedtakslengdeUtvidelse.Automatisk(
            forrigeSluttdato = dagensDato,
            nySluttdato = dagensDato.plusYears(1),
        )
        every { behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId) } returns behandlingMedVedtak()
        every { flytJobbRepository.hentJobberForSak(sakId.toLong()) } returns listOf(annenJobb)
        every { flytJobbRepository.leggTil(match { it.sakId() == sakId.id }) } just Runs

        opprettJobbUtfører().utfør(jobbInput)

        val slot = slot<JobbInput>()
        verify(exactly = 1) { flytJobbRepository.leggTil(capture(slot)) }

        assertThat(slot.captured)
            .usingRecursiveComparison()
            .isEqualTo(jobbInputSak)
    }

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

}