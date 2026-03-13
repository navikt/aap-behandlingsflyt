package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpprettJobbForFritakMeldepliktJobbUtførerTest {
    private val sakId = SakId(12345L)
    private val flytJobbRepository = mockk<FlytJobbRepository>()
    private val sakRepository = mockk<SakRepository>()
    private val jobbInput = JobbInput(OpprettJobbForFritakMeldepliktJobbUtfører).forSak(sakId.toLong())

    @Test
    fun `Kjører mandager i helligdagstilfeller`() {
        val enMandagHelligPeriode = LocalDate.of(2026, 3, 30)
        val clock = fixedClock(enMandagHelligPeriode)
        val jobbUtfører = OpprettJobbForFritakMeldepliktJobbUtfører(flytJobbRepository, sakRepository, clock)

        every { sakRepository.finnSakerMedFritakMeldeplikt() } returns listOf(sakId)
        every { flytJobbRepository.leggTil((any()))} just Runs

        jobbUtfører.utfør(jobbInput)

        verify(exactly = 1) { flytJobbRepository.leggTil((any())) }
    }

    @Test
    fun `Kjører ikke mandager når det ikke er en helligdagsperiode`() {
        val enMandagIkkeHelligPeriode = LocalDate.of(2026, 3, 23)
        val clock = fixedClock(enMandagIkkeHelligPeriode)
        val jobbUtfører = OpprettJobbForFritakMeldepliktJobbUtfører(flytJobbRepository, sakRepository, clock)

        every { sakRepository.finnSakerMedFritakMeldeplikt() } returns listOf(sakId)
        every { flytJobbRepository.leggTil((any()))} just Runs

        jobbUtfører.utfør(jobbInput)

        verify(exactly = 0) { flytJobbRepository.leggTil((any())) }

        jobbUtfører.utfør(jobbInput)
    }
}