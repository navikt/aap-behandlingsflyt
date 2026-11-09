package no.nav.aap.meldeplikt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate


class HelligdagsunntakMeldekortTest {

    @Test
    fun `nye fritaksdatoer finnes bare utenfor prod`() {

        val prod = helligdagsunntakFritaksUtbetalingDato(erProd = true)
        val ikkeProd = helligdagsunntakFritaksUtbetalingDato(erProd = false)

        val forventetBareUtenforProd = mapOf(
            LocalDate.of(2026, 11, 25) to LocalDate.of(2026, 11, 23),
            LocalDate.of(2026, 12, 2) to LocalDate.of(2026, 11, 30),
            LocalDate.of(2026, 12, 9) to LocalDate.of(2026, 12, 7),
            LocalDate.of(2026, 12, 16) to LocalDate.of(2026, 12, 14),
            LocalDate.of(2026, 12, 23) to LocalDate.of(2026, 12, 21),
            LocalDate.of(2026, 12, 30) to LocalDate.of(2026, 12, 28),
        )

        assertThat(ikkeProd).containsAllEntriesOf(prod)
        assertThat(ikkeProd - prod.keys).isEqualTo(forventetBareUtenforProd)
    }

    @Test
    fun `nye fastsatte meldedager finnes bare utenfor prod`() {

        val prod = helligdagsunntakFastsattMeldedag(erProd = true)
        val ikkeProd = helligdagsunntakFastsattMeldedag(erProd = false)

        val forventetBareUtenforProd = mapOf(
            LocalDate.of(2026, 11, 30) to LocalDate.of(2026, 11, 25),
            LocalDate.of(2026, 12, 7) to LocalDate.of(2026, 12, 2),
            LocalDate.of(2026, 12, 21) to LocalDate.of(2026, 12, 16),
        )

        assertThat(ikkeProd).containsAllEntriesOf(prod)
        assertThat(ikkeProd - prod.keys).isEqualTo(forventetBareUtenforProd)
    }
}