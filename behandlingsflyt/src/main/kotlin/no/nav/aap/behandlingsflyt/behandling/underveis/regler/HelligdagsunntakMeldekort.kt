package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

/**
 * Map fra ordinær fastsatt dag til justert dato for helligdager. Se [Confluence](https://confluence.adeo.no/x/uizUL) for dokumentasjon.
 */
val helligdagsunntakFastsattMeldedag =
    mapOf(
        LocalDate.of(2025, 12, 22) to LocalDate.of(2025, 12, 17)
    )

val helligdagsunntakFritaksUtbetalingDato =
    mapOf(
        LocalDate.of(2025, 12, 17) to LocalDate.of(2025, 12, 15),
        LocalDate.of(2025, 12, 24) to LocalDate.of(2025, 12, 22),
        LocalDate.of(2025, 12, 31) to LocalDate.of(2025, 12, 29),
        LocalDate.of(2026, 4, 1) to LocalDate.of(2026, 3, 30),
        LocalDate.of(2026, 5, 13) to LocalDate.of(2026, 5, 12)
    )

/**
 * Sjekker om en meldeperiode inneholder et helligdagsunntak for å
 * kunne beregne meldekort for "fritak for meldeplikt"-brukere.
 */
fun erHelligdagsUnntakPeriode(periode: Periode): Boolean {
    val originaleDatoer = helligdagsunntakFritaksUtbetalingDato.keys
    return originaleDatoer.any { periode.inneholder(it) }
}

private val helligdagsunntakMeldefrist =
    mapOf(
        LocalDate.of(2026, 4, 6) to LocalDate.of(2026, 4, 7),
        LocalDate.of(2026, 5, 25) to LocalDate.of(2026, 5, 26),
    )

fun helligdagsunntakjustertMeldefrist(dato: LocalDate): LocalDate {
    return helligdagsunntakMeldefrist[dato] ?: dato
}