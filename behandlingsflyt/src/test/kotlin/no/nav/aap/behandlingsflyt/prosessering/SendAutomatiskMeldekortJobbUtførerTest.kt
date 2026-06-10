package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.TestAutomatiskMeldekortSakRepository
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class SendAutomatiskMeldekortJobbUtførerTest {

    private val sakId = SakId(12345L)
    private val behandlingId = BehandlingId(456L)
    private val jobbInput = JobbInput(SendAutomatiskMeldekortJobbUtfører).forSak(sakId.toLong())

    @BeforeEach
    fun setup() {
        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
    }

    private val automatiskMeldekortSakRepository = mockk<TestAutomatiskMeldekortSakRepository>()
    private val behandlingService = mockk<BehandlingService>()
    private val rettighetstypeRepository = mockk<RettighetstypeRepository>()
    private val flytJobbRepository = mockk<FlytJobbRepository>()

    @Test
    fun `sender meldekort når sak har aktiv rettighetstype i dag`() {
        val idag = LocalDate.of(2026, 6, 9)
        val utfører = lagUtfører(idag)

        every { automatiskMeldekortSakRepository.hentAlle() } returns listOf(sakId)
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns lagBehandling()
        every { rettighetstypeRepository.hentHvisEksisterer(behandlingId) } returns lagRettighetstypeGrunnlag(
            fom = idag.minusDays(30),
            tom = idag.plusDays(30),
        )
        every { flytJobbRepository.leggTil(any()) } just Runs

        utfører.utfør(jobbInput)

        verify(exactly = 1) { flytJobbRepository.leggTil(any()) }
    }

    @Test
    fun `sender ikke meldekort når rettighetstidslinja ikke dekker i dag`() {
        val idag = LocalDate.of(2026, 6, 9)
        val utfører = lagUtfører(idag)

        every { automatiskMeldekortSakRepository.hentAlle() } returns listOf(sakId)
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns lagBehandling()
        every { rettighetstypeRepository.hentHvisEksisterer(behandlingId) } returns lagRettighetstypeGrunnlag(
            fom = idag.minusDays(60),
            tom = idag.minusDays(1),
        )

        utfører.utfør(jobbInput)

        verify(exactly = 0) { flytJobbRepository.leggTil(any()) }
    }

    @Test
    fun `meldekort-jobb opprettes med referanse av type JOURNALPOST`() {
        val idag = LocalDate.of(2026, 6, 9)
        val utfører = lagUtfører(idag)

        every { automatiskMeldekortSakRepository.hentAlle() } returns listOf(sakId)
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns lagBehandling()
        every { rettighetstypeRepository.hentHvisEksisterer(behandlingId) } returns lagRettighetstypeGrunnlag(
            fom = idag.minusDays(30),
            tom = idag.plusDays(30),
        )
        every { flytJobbRepository.leggTil(any()) } just Runs

        utfører.utfør(jobbInput)

        val slot = slot<JobbInput>()
        verify(exactly = 1) { flytJobbRepository.leggTil(capture(slot)) }
        val referanse = DefaultJsonMapper.fromJson<InnsendingReferanse>(slot.captured.parameter("referanse"))
        assert(referanse.type == InnsendingReferanse.Type.JOURNALPOST) {
            "Forventer JOURNALPOST-type, men fikk: ${referanse.type}"
        }
    }

    @Test
    fun `sender meldekort for flere saker`() {
        val idag = LocalDate.of(2026, 6, 9)
        val annenSakId = SakId(99999L)
        val annenBehandlingId = BehandlingId(777L)
        val utfører = lagUtfører(idag)

        every { automatiskMeldekortSakRepository.hentAlle() } returns listOf(sakId, annenSakId)
        every { behandlingService.finnSisteYtelsesbehandlingFor(sakId) } returns lagBehandling()
        every { behandlingService.finnSisteYtelsesbehandlingFor(annenSakId) } returns lagBehandling(annenBehandlingId, annenSakId)
        every { rettighetstypeRepository.hentHvisEksisterer(behandlingId) } returns lagRettighetstypeGrunnlag(
            fom = idag.minusDays(30),
            tom = idag.plusDays(30),
        )
        every { rettighetstypeRepository.hentHvisEksisterer(annenBehandlingId) } returns lagRettighetstypeGrunnlag(
            fom = idag.minusDays(10),
            tom = idag.plusDays(10),
        )
        every { flytJobbRepository.leggTil(any()) } just Runs

        utfører.utfør(jobbInput)

        verify(exactly = 2) { flytJobbRepository.leggTil(any()) }
    }

    private fun lagUtfører(idag: LocalDate) = SendAutomatiskMeldekortJobbUtfører(
        automatiskMeldekortSakRepository = automatiskMeldekortSakRepository,
        behandlingService = behandlingService,
        rettighetstypeRepository = rettighetstypeRepository,
        flytJobbRepository = flytJobbRepository,
        clock = fixedClock(idag),
    )

    private fun lagBehandling(
        id: BehandlingId = behandlingId,
        sakId: SakId = this.sakId,
    ) = Behandling(
        id = id,
        forrigeBehandlingId = null,
        referanse = BehandlingReferanse(UUID.randomUUID()),
        sakId = sakId,
        typeBehandling = TypeBehandling.Førstegangsbehandling,
        status = Status.AVSLUTTET,
        vurderingsbehov = emptyList(),
        stegTilstand = null,
        årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
        versjon = 0L,
    )

    private fun lagRettighetstypeGrunnlag(fom: LocalDate, tom: LocalDate) = RettighetstypeGrunnlag(
        rettighetstypeTidslinje = Tidslinje(
            listOf(Segment(Periode(fom, tom), RettighetsType.BISTANDSBEHOV))
        )
    )
}
