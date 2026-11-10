package no.nav.aap.meldeplikt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.to


class HelligdagsunntakMeldekortTest {

    /** Sikkerhetsnett for å forsøke å redusere single point of failure for tilbakekreving
     * ved endringer på unntaksdatoer i HelligdagsunntakMeldekort.kt
     *
     * Legg til ny test for hver nye unntaksdato-tupel som legges til i HelligdagsunntakMeldekort.kt
     */
    @Test
    fun `prodmiljø har fritaksunntak for 2025-12-17`() {
        val prodUnntak = helligdagsunntakFritaksUtbetalingDato(erProd = true)
        assertThat(prodUnntak).containsEntry(LocalDate.of(2025, 12, 17), LocalDate.of(2025, 12, 15))
    }

    @Test
    fun `prodmiljø har fritaksunntak for 2025-12-24`() {
        val prodUnntak = helligdagsunntakFritaksUtbetalingDato(erProd = true)
        assertThat(prodUnntak).containsEntry(LocalDate.of(2025, 12, 24), LocalDate.of(2025, 12, 22))
    }

    @Test
    fun `prodmiljø har fritaksunntak for 2025-12-31`() {
        val prodUnntak = helligdagsunntakFritaksUtbetalingDato(erProd = true)
        assertThat(prodUnntak).containsEntry(LocalDate.of(2025, 12, 31), LocalDate.of(2025, 12, 29))
    }

    @Test
    fun `prodmiljø har fritaksunntak for 2026-04-01`() {
        val prodUnntak = helligdagsunntakFritaksUtbetalingDato(erProd = true)
        assertThat(prodUnntak).containsEntry(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 3, 30))
    }

    @Test
    fun `prodmiljø har fritaksunntak for 2026-05-13`() {
        val prodUnntak = helligdagsunntakFritaksUtbetalingDato(erProd = true)
        assertThat(prodUnntak).containsEntry(LocalDate.of(2026, 5, 13), LocalDate.of(2026, 5, 12))
    }

    @Test
    fun `prodmiljø har meldedagsunntak for 2025-12-22`() {
        val prodUnntak = helligdagsunntakFastsattMeldedag(erProd = true)
        assertThat(prodUnntak).containsEntry(LocalDate.of(2025, 12, 22), LocalDate.of(2025, 12, 17))
    }

    @Test
    fun `prodmiljø har et forventet unntaksutvalg for fritaksdatoer`() {
        val prodUnntak = helligdagsunntakFritaksUtbetalingDato(erProd = true)

        val forventetUtvalg = mapOf(
            LocalDate.of(2025, 12, 17) to LocalDate.of(2025, 12, 15),
            LocalDate.of(2025, 12, 24) to LocalDate.of(2025, 12, 22),
            LocalDate.of(2025, 12, 31) to LocalDate.of(2025, 12, 29),
            LocalDate.of(2026, 4, 1) to LocalDate.of(2026, 3, 30),
            LocalDate.of(2026, 5, 13) to LocalDate.of(2026, 5, 12),
        )

        assertThat(prodUnntak).isEqualTo(forventetUtvalg)
    }

    @Test
    fun `testmiljø har et forventet unntaksutvalg for fritaksdatoer`() {
        val testUnntak = helligdagsunntakFritaksUtbetalingDato(erProd = false)

        val forventetUtvalg = mapOf(
            LocalDate.of(2025, 12, 17) to LocalDate.of(2025, 12, 15),
            LocalDate.of(2025, 12, 24) to LocalDate.of(2025, 12, 22),
            LocalDate.of(2025, 12, 31) to LocalDate.of(2025, 12, 29),
            LocalDate.of(2026, 4, 1) to LocalDate.of(2026, 3, 30),
            LocalDate.of(2026, 5, 13) to LocalDate.of(2026, 5, 12),

            LocalDate.of(2026, 12, 16) to LocalDate.of(2026, 12, 14),
            LocalDate.of(2026, 12, 23) to LocalDate.of(2026, 12, 21),
            LocalDate.of(2026, 12, 30) to LocalDate.of(2026, 12, 28),
            LocalDate.of(2026, 11, 25) to LocalDate.of(2026, 11, 23),
            LocalDate.of(2026, 12, 2) to LocalDate.of(2026, 11, 30),
            LocalDate.of(2026, 12, 9) to LocalDate.of(2026, 12, 7),
        )

        assertThat(testUnntak).isEqualTo(forventetUtvalg)
    }

    @Test
    fun `prodmiljø har et forventet unntaksutvalg for meldedager`() {
        val prodUnntak = helligdagsunntakFastsattMeldedag(erProd = true)

        val forventetUtvalg = mapOf(
            LocalDate.of(2025, 12, 22) to LocalDate.of(2025, 12, 17),
        )

        assertThat(prodUnntak).isEqualTo(forventetUtvalg)
    }

    @Test
    fun `testmiljø har et forventet unntaksutvalg for meldedager`() {
        val testUnntak = helligdagsunntakFastsattMeldedag(erProd = false)

        val forventetUnntak = mapOf(
            LocalDate.of(2025, 12, 22) to LocalDate.of(2025, 12, 17),
            LocalDate.of(2026, 12, 21) to LocalDate.of(2026, 12, 16),
            LocalDate.of(2026, 11, 30) to LocalDate.of(2026, 11, 25),
            LocalDate.of(2026, 12, 7) to LocalDate.of(2026, 12, 2),
        )

        assertThat(testUnntak).isEqualTo(forventetUnntak)
    }

}