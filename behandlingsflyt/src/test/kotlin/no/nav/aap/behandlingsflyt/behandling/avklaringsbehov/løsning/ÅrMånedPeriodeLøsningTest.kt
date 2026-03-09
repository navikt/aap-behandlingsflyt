package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.årmåned.validerPerioder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.årmåned.ÅrMånedPeriodeLøsning
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.YearMonth

class ÅrMånedPeriodeLøsningTest {
    data class P(
        override val fom: YearMonth,
        override val tom: YearMonth?,
    ) : ÅrMånedPeriodeLøsning

    @Test
    fun `ingen perioder er ok`() {
        assertDoesNotThrow {
            validerPerioder(listOf())
        }
    }

    @Test
    fun `en periode på 1 måned med eksplisitt start og slutt er ok`() {
        assertDoesNotThrow {
            validerPerioder(
                listOf(
                    P(YearMonth.of(2025, 1), YearMonth.of(2025, 1))
                )
            )
        }
    }

    @Test
    fun `en periode med implisitt slutt er ok`() {
        assertDoesNotThrow {
            validerPerioder(
                listOf(
                    P(YearMonth.of(2025, 1), null)
                )
            )
        }
    }

    @Test
    fun `to en-måneds-perioder rett etter hverandre er ok`() {
        assertDoesNotThrow {
            validerPerioder(
                listOf(
                    P(YearMonth.of(2025, 1), YearMonth.of(2025, 1)),
                    P(YearMonth.of(2025, 2), YearMonth.of(2025, 2)),
                )
            )
        }
    }

    @Test
    fun `hull mellom vurderingen er ok`() {
        assertDoesNotThrow {
            validerPerioder(
                listOf(
                    P(YearMonth.of(2025, 1), YearMonth.of(2025, 1)),
                    P(YearMonth.of(2025, 3), YearMonth.of(2025, 3)),
                )
            )
        }
    }

    @Test
    fun `rekkefølge er ubetydelig ved eksplisitt slutt-dato`() {
        assertDoesNotThrow {
            validerPerioder(
                listOf(
                    /* "baklengs" */
                    P(YearMonth.of(2025, 2), YearMonth.of(2025, 2)),
                    P(YearMonth.of(2025, 1), YearMonth.of(2025, 1)),
                )
            )
        }
    }

    @Test
    fun `to helt like perioder med implisitt slutt er ikke ok`() {
        assertThrows<UgyldigForespørselException> {
            validerPerioder(
                listOf(
                    P(YearMonth.of(2025, 1), null),
                    P(YearMonth.of(2025, 1), null),
                )
            )
        }
    }

    @Test
    fun `to helt like perioder med eksplisitt slutt er ikke ok`() {
        assertThrows<UgyldigForespørselException> {
            validerPerioder(
                listOf(
                    P(YearMonth.of(2025, 1), YearMonth.of(2025, 1)),
                    P(YearMonth.of(2025, 1), YearMonth.of(2025, 1)),
                )
            )
        }
    }

    @Test
    fun `flere implisitte slutt-datoer er ikke ok `() {
        assertThrows<UgyldigForespørselException> {
            validerPerioder(
                listOf(
                    P(YearMonth.of(2025, 1), null),
                    P(YearMonth.of(2025, 2), null),
                )
            )
        }
    }

    @Test
    fun `blande implisitte og eksplisitte sluttdatoer er ok `() {
        assertDoesNotThrow {
            validerPerioder(
                listOf(
                    P(YearMonth.of(2022, 3), YearMonth.of(2022, 6)),
                    P(YearMonth.of(2025, 1), null),
                )
            )
        }
    }
}