package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.gregulering.GReguleringService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Year

class OpprettJobbForGReguleringJobbUtførerTest {

    private val dagensDato = 1 juni 2025
    private val clock = fixedClock(dagensDato)

    private val gReguleringService = mockk<GReguleringService>()
    private val flytJobbRepository = mockk<FlytJobbRepository>()
    private val unleashGateway = mockk<UnleashGateway>()

    private val jobbInput = JobbInput(OpprettJobbForGReguleringJobbUtfører)

    private fun opprettUtfører() =
        OpprettJobbForGReguleringJobbUtfører(
            gReguleringService = gReguleringService,
            flytJobbRepository = flytJobbRepository,
            clock = clock,
            unleashGateway = unleashGateway,
        )

    private fun enableToggle() {
        every { unleashGateway.isDisabled(BehandlingsflytFeature.GReguleringUtplukkJobb) } returns false
    }

    private fun disableToggle() {
        every { unleashGateway.isDisabled(BehandlingsflytFeature.GReguleringUtplukkJobb) } returns true
    }

    @Test
    fun `skal ikke opprette jobber når feature toggle er avskrudd`() {
        disableToggle()

        opprettUtfører().utfør(jobbInput)

        verify(exactly = 0) { gReguleringService.finnesGrunnbeløpForÅr(any()) }
        verify(exactly = 0) { gReguleringService.hentSakerForGRegulering(any()) }
        verify(exactly = 0) { flytJobbRepository.leggTil(any()) }
    }

    @Test
    fun `skal ikke opprette jobber når ingen G-justering finnes for inneværende år`() {
        enableToggle()
        every { gReguleringService.finnesGrunnbeløpForÅr(Year.of(2025)) } returns null

        opprettUtfører().utfør(jobbInput)

        verify(exactly = 0) { gReguleringService.hentSakerForGRegulering(any()) }
        verify(exactly = 0) { flytJobbRepository.leggTil(any()) }
    }

    @Test
    fun `skal opprette jobber for kandidater når feature toggle er på`() {
        enableToggle()

        val gjusteringDato = LocalDate.of(2025, 5, 1)
        val sakId = SakId(42L)

        every { gReguleringService.finnesGrunnbeløpForÅr(Year.of(2025)) } returns
            Grunnbeløp.GrunnbeløpDto(dato = gjusteringDato, beløp = Beløp(130_160))
        every { gReguleringService.hentSakerForGRegulering(gjusteringDato) } returns setOf(sakId)
        every { flytJobbRepository.leggTil(any()) } just Runs

        opprettUtfører().utfør(jobbInput)

        verify(exactly = 1) { flytJobbRepository.leggTil(any()) }
    }
}
