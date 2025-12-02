package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.LøsningForPeriode
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class AvklaringsbehovApiTest {
    data class P(
        override val fom: LocalDate,
        override val tom: LocalDate?,
    ) : LøsningForPeriode {
        override val begrunnelse = "$fom–${tom ?: "…"}"
    }

    @Test
    fun `ingen perioder er ok`() {
        assertDoesNotThrow {
            validerPerioder(listOf())
        }
    }

    @Test
    fun `en periode på 1 dag med eksplisitt start og slutt er ok`() {
        assertDoesNotThrow {
            validerPerioder(listOf(
                P(1 januar 2025, 1 januar 2025)
            ))
        }
    }

    @Test
    fun `en periode med implisitt slutt er ok`() {
        assertDoesNotThrow {
            validerPerioder(listOf(
                P(1 januar 2025, null)
            ))
        }
    }

    @Test
    fun `to en-dags-perioder rett etter hverandre er ok`() {
        assertDoesNotThrow {
            validerPerioder(listOf(
                P(1 januar 2025, 1 januar 2025),
                P(2 januar 2025, 2 januar 2025),
            ))
        }
    }

    @Test
    fun `hull mellom vurderingen er ok`() {
        assertDoesNotThrow {
            validerPerioder(listOf(
                P(1 januar 2025, 1 januar 2025),
                P(10 januar 2025, 15 januar 2025),
            ))
        }
    }

    @Test
    fun `rekkefølge er ubetydelig ved eksplisitt slutt-dato`() {
        assertDoesNotThrow {
            validerPerioder(listOf(
                /* "baklengs" */
                P(2 januar 2025, 2 januar 2025),
                P(1 januar 2025, 1 januar 2025),
            ))
        }
    }

    @Test
    fun `to helt like perioder med implisitt slutt er ikke ok`() {
        assertThrows<UgyldigForespørselException> {
            validerPerioder(listOf(
                P(1 januar 2025, null),
                P(1 januar 2025, null),
            ))
        }
    }

    @Test
    fun `to helt like perioder med eksplisitt slutt er ikke ok`() {
        assertThrows<UgyldigForespørselException> {
            validerPerioder(listOf(
                P(1 januar 2025, 1 januar 2025),
                P(1 januar 2025, 1 januar 2025),
            ))
        }
    }

    @Test
    fun `flere implisitte slutt-datoer er ok `() {
        assertDoesNotThrow {
            validerPerioder(listOf(
                P(1 januar 2025, null),
                P(2 januar 2025, null),
            ))
        }
    }

    @Test
    fun `blande implisitte og eksplisitte sluttdatoer er ok `() {
        assertDoesNotThrow {
            validerPerioder(listOf(
                P(3 januar 2022, 6 januar 2022),
                P(1 januar 2025, null),
            ))
        }
    }
}