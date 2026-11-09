package no.nav.aap.meldeplikt

import no.nav.aap.komponenter.miljo.Miljø
import java.time.LocalDate

/**
 * Map fra ordinær fastsatt dag til justert dato for helligdager. Se [Confluence](https://confluence.adeo.no/x/uizUL) for dokumentasjon.
 */
fun helligdagsunntakFastsattMeldedag(): Map<LocalDate, LocalDate> = if (Miljø.erProd())
    mapOf(
        LocalDate.of(2025, 12, 22) to LocalDate.of(2025, 12, 17)
    ) else mapOf(
        LocalDate.of(2026, 11, 30) to LocalDate.of(2026, 12, 25),
        LocalDate.of(2026, 12, 7) to LocalDate.of(2026, 12, 2),
        LocalDate.of(2026, 12, 21) to LocalDate.of(2026, 12, 16),
    )

fun helligdagsunntakFritaksUtbetalingDato(): Map<LocalDate, LocalDate> = if (Miljø.erProd())
    mapOf(
        LocalDate.of(2025, 12, 17) to LocalDate.of(2025, 12, 15),
        LocalDate.of(2025, 12, 24) to LocalDate.of(2025, 12, 22),
        LocalDate.of(2025, 12, 31) to LocalDate.of(2025, 12, 29),
        LocalDate.of(2026, 4, 1) to LocalDate.of(2026, 3, 30),
        LocalDate.of(2026, 5, 13) to LocalDate.of(2026, 5, 12)
    ) else mapOf(
        LocalDate.of(2025, 12, 17) to LocalDate.of(2025, 12, 15),
        LocalDate.of(2025, 12, 24) to LocalDate.of(2025, 12, 22),
        LocalDate.of(2025, 12, 31) to LocalDate.of(2025, 12, 29),
        LocalDate.of(2026, 4, 1) to LocalDate.of(2026, 3, 30),
        LocalDate.of(2026, 5, 13) to LocalDate.of(2026, 5, 12),

        LocalDate.of(2026, 11, 25) to LocalDate.of(2026, 11, 23),
        LocalDate.of(2026, 12, 2) to LocalDate.of(2026, 11, 30),
        LocalDate.of(2026, 12, 9) to LocalDate.of(2026, 12, 7),
        LocalDate.of(2026, 12, 16) to LocalDate.of(2026, 12, 14),
        LocalDate.of(2026, 12, 23) to LocalDate.of(2026, 12, 21),
        LocalDate.of(2026, 12, 30) to LocalDate.of(2026, 12, 28),
    )

/**
 * Sjekker om en meldeperiode inneholder et helligdagsunntak for å
 * kunne beregne meldekort for "fritak for meldeplikt"-brukere.
 */
fun erHelligdagsUnntak(dato: LocalDate): Boolean {
    val unntaksDatoer = helligdagsunntakFritaksUtbetalingDato().values
    return dato in unntaksDatoer
}

private val helligdagsunntakMeldefrist =
    mapOf(
        LocalDate.of(2026, 4, 6) to LocalDate.of(2026, 4, 7),
        LocalDate.of(2026, 5, 25) to LocalDate.of(2026, 5, 26),
    )

fun helligdagsunntakjustertMeldefrist(dato: LocalDate): LocalDate {
    return helligdagsunntakMeldefrist[dato] ?: dato
}