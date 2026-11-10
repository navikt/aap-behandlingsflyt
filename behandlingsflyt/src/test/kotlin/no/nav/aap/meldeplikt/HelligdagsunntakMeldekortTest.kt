package no.nav.aap.meldeplikt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate


class HelligdagsunntakMeldekortTest {

    @Test
    fun `fritaksdatoer har forventede verdier i prod og tillegg utenfor prod`() {

        val prod = helligdagsunntakFritaksUtbetalingDato(erProd = true)
        val ikkeProd = helligdagsunntakFritaksUtbetalingDato(erProd = false)

        val forventetProd = mapOf(
            LocalDate.of(2025, 12, 17) to LocalDate.of(2025, 12, 15),
            LocalDate.of(2025, 12, 24) to LocalDate.of(2025, 12, 22),
            LocalDate.of(2025, 12, 31) to LocalDate.of(2025, 12, 29),
            LocalDate.of(2026, 4, 1) to LocalDate.of(2026, 3, 30),
            LocalDate.of(2026, 5, 13) to LocalDate.of(2026, 5, 12),
        )
        val forventetBareUtenforProd = mapOf(
            LocalDate.of(2026, 12, 16) to LocalDate.of(2026, 12, 14),
            LocalDate.of(2026, 12, 23) to LocalDate.of(2026, 12, 21),
            LocalDate.of(2026, 12, 30) to LocalDate.of(2026, 12, 28),
            LocalDate.of(2026, 11, 25) to LocalDate.of(2026, 11, 23),
            LocalDate.of(2026, 12, 2) to LocalDate.of(2026, 11, 30),
            LocalDate.of(2026, 12, 9) to LocalDate.of(2026, 12, 7),
        )

        assertThat(prod).isEqualTo(forventetProd)
        assertThat(ikkeProd).isEqualTo(prod + forventetBareUtenforProd)
    }

    @Test
    fun `fastsatte meldedager har forventede verdier i prod og tillegg utenfor prod`() {

        val prod = helligdagsunntakFastsattMeldedag(erProd = true)
        val ikkeProd = helligdagsunntakFastsattMeldedag(erProd = false)

        val forventetProd = mapOf(
            LocalDate.of(2025, 12, 22) to LocalDate.of(2025, 12, 17),
        )
        val forventetBareUtenforProd = mapOf(
            LocalDate.of(2026, 12, 21) to LocalDate.of(2026, 12, 16),
            LocalDate.of(2026, 11, 30) to LocalDate.of(2026, 11, 25),
            LocalDate.of(2026, 12, 7) to LocalDate.of(2026, 12, 2),
        )

        assertThat(prod).isEqualTo(forventetProd)
        assertThat(ikkeProd).isEqualTo(prod + forventetBareUtenforProd)
    }

    @Test
    fun `helligdagsunntak samsvarer med aktiv fritaksmap`() {
        assertThat(aktiveHelligdagsunntakForFritaksutbetalingDatoer.values).allSatisfy { dato ->
            assertThat(erHelligdagsUnntak(dato)).isTrue()
        }
        assertThat(erHelligdagsUnntak(LocalDate.of(2026, 11, 24))).isFalse()
    }
}