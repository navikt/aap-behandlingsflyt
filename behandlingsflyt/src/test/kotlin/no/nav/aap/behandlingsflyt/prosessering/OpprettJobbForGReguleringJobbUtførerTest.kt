package no.nav.aap.behandlingsflyt.prosessering

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.gregulering.GReguleringService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.fixedClock
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.assertj.core.api.Assertions.assertThat
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

    private fun enableToggleWithFilter(vararg sakIds: Long) {
        every { unleashGateway.isDisabled(BehandlingsflytFeature.GReguleringUtplukkJobb) } returns false
        every { unleashGateway.hentSakIdFilter(BehandlingsflytFeature.GReguleringUtplukkJobb) } returns sakIds.toSet()
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
    fun `skal opprette jobber for kandidater når feature toggle er på`() {
        val gjusteringDato = LocalDate.of(2025, 5, 1)
        val sakId = SakId(42L)
        enableToggleWithFilter(sakId.id)

        every { gReguleringService.finnesGrunnbeløpForÅr(Year.of(2025)) } returns
            Grunnbeløp.GrunnbeløpMedDato(dato = gjusteringDato, beløp = Beløp(130_160))
        every { gReguleringService.hentSakerForGRegulering(gjusteringDato) } returns setOf(sakId)
        every { flytJobbRepository.leggTil(any()) } just Runs

        opprettUtfører().utfør(jobbInput)

        verify(exactly = 1) { flytJobbRepository.leggTil(any()) }
    }

    @Test
    fun `skal ikke opprette jobber når sak-id ikke er i filterlisten`() {
        val gjusteringDato = LocalDate.of(2025, 5, 1)
        val sakId = SakId(42L)
        enableToggleWithFilter(99L) // Kun sak 99 er tillatt, ikke 42

        every { gReguleringService.finnesGrunnbeløpForÅr(Year.of(2025)) } returns
            Grunnbeløp.GrunnbeløpMedDato(dato = gjusteringDato, beløp = Beløp(130_160))
        every { gReguleringService.hentSakerForGRegulering(gjusteringDato) } returns setOf(sakId)

        opprettUtfører().utfør(jobbInput)

        verify(exactly = 0) { flytJobbRepository.leggTil(any()) }
    }

    @Test
    fun `skal bruke forrige års G-justering når dagsdato er i januar (før 1 mai)`() {
        val gjusteringDato = LocalDate.of(2026, 5, 1)
        val sakId = SakId(99L)
        enableToggleWithFilter(sakId.id)

        val utfører = OpprettJobbForGReguleringJobbUtfører(
            gReguleringService = gReguleringService,
            flytJobbRepository = flytJobbRepository,
            clock = fixedClock(15 januar 2027),
            unleashGateway = unleashGateway,
        )

        // 15. jan 2027 → G-periode-år = 2026 → leter etter 2026-G-justeringen
        every { gReguleringService.finnesGrunnbeløpForÅr(Year.of(2026)) } returns
            Grunnbeløp.GrunnbeløpMedDato(dato = gjusteringDato, beløp = Beløp(135_000))
        every { gReguleringService.hentSakerForGRegulering(gjusteringDato) } returns setOf(sakId)
        every { flytJobbRepository.leggTil(any()) } just Runs

        utfører.utfør(jobbInput)

        verify(exactly = 1) { flytJobbRepository.leggTil(any()) }
        verify(exactly = 1) { gReguleringService.finnesGrunnbeløpForÅr(Year.of(2026)) }
        verify(exactly = 0) { gReguleringService.finnesGrunnbeløpForÅr(Year.of(2027)) }
    }

    @Test
    fun `skal bruke inneværende års G-justering når dagsdato er 1 mai eller etter`() {
        val gjusteringDato = LocalDate.of(2027, 5, 1)
        val sakId = SakId(77L)
        enableToggleWithFilter(sakId.id)

        val utfører = OpprettJobbForGReguleringJobbUtfører(
            gReguleringService = gReguleringService,
            flytJobbRepository = flytJobbRepository,
            clock = fixedClock(1 mai 2027),
            unleashGateway = unleashGateway,
        )

        // 1. mai 2027 → G-periode-år = 2027 → leter etter 2027-G-justeringen
        every { gReguleringService.finnesGrunnbeløpForÅr(Year.of(2027)) } returns
            Grunnbeløp.GrunnbeløpMedDato(dato = gjusteringDato, beløp = Beløp(140_000))
        every { gReguleringService.hentSakerForGRegulering(gjusteringDato) } returns setOf(sakId)
        every { flytJobbRepository.leggTil(any()) } just Runs

        utfører.utfør(jobbInput)

        verify(exactly = 1) { flytJobbRepository.leggTil(any()) }
        verify(exactly = 0) { gReguleringService.finnesGrunnbeløpForÅr(Year.of(2026)) }
        verify(exactly = 1) { gReguleringService.finnesGrunnbeløpForÅr(Year.of(2027)) }
    }

    @Test
    fun `gPeriodeÅr - dato i januar gir forrige år`() {
        val utfører = opprettUtfører()
        assertThat(utfører.gPeriodeÅr(15 januar 2027)).isEqualTo(Year.of(2026))
    }

    @Test
    fun `gPeriodeÅr - dato 30 april gir forrige år`() {
        val utfører = opprettUtfører()
        assertThat(utfører.gPeriodeÅr(30 april 2027)).isEqualTo(Year.of(2026))
    }

    @Test
    fun `gPeriodeÅr - dato 1 mai gir inneværende år`() {
        val utfører = opprettUtfører()
        assertThat(utfører.gPeriodeÅr(1 mai 2027)).isEqualTo(Year.of(2027))
    }

    @Test
    fun `gPeriodeÅr - dato i desember gir inneværende år`() {
        val utfører = opprettUtfører()
        assertThat(utfører.gPeriodeÅr(31 desember 2027)).isEqualTo(Year.of(2027))
    }
}
