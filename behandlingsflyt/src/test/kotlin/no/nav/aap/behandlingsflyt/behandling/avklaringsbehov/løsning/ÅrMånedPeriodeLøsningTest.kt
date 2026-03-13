package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.årmåned.validerPerioder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.årmåned.ÅrMånedPeriodeLøsning
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class ÅrMånedPeriodeLøsningTest {
    data class P(
        override val fom: String,
        override val tom: String?,
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
                    P("2025-01", "2025-01")
                )
            )
        }
    }

    @Test
    fun `en periode med implisitt slutt er ok`() {
        assertDoesNotThrow {
            validerPerioder(
                listOf(
                    P("2025-01", null)
                )
            )
        }
    }

    @Test
    fun `to en-måneds-perioder rett etter hverandre er ok`() {
        assertDoesNotThrow {
            validerPerioder(
                listOf(
                    P("2025-01", "2025-01"),
                    P("2025-02", "2025-02"),
                )
            )
        }
    }

    @Test
    fun `hull mellom vurderingen er ok`() {
        assertDoesNotThrow {
            validerPerioder(
                listOf(
                    P("2025-01", "2025-01"),
                    P("2025-03", "2025-03"),
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
                    P("2025-02", "2025-02"),
                    P("2025-01", "2025-01"),
                )
            )
        }
    }

    @Test
    fun `to helt like perioder med implisitt slutt er ikke ok`() {
        assertThrows<UgyldigForespørselException> {
            validerPerioder(
                listOf(
                    P("2025-01", null),
                    P("2025-01", null),
                )
            )
        }
    }

    @Test
    fun `to helt like perioder med eksplisitt slutt er ikke ok`() {
        assertThrows<UgyldigForespørselException> {
            validerPerioder(
                listOf(
                    P("2025-01", "2025-01"),
                    P("2025-01", "2025-01"),
                )
            )
        }
    }

    @Test
    fun `flere implisitte slutt-datoer er ikke ok `() {
        assertThrows<UgyldigForespørselException> {
            validerPerioder(
                listOf(
                    P("2025-01", null),
                    P("2025-02", null),
                )
            )
        }
    }

    @Test
    fun `blande implisitte og eksplisitte sluttdatoer er ok `() {
        assertDoesNotThrow {
            validerPerioder(
                listOf(
                    P("2022-03", "2022-06"),
                    P("2025-01", null),
                )
            )
        }
    }
}