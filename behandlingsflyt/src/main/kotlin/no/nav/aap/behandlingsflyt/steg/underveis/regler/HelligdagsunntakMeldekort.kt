package no.nav.aap.behandlingsflyt.steg.underveis.regler

import no.nav.aap.underveis.helligdagsunntakFritaksUtbetalingDato
import java.time.LocalDate

/**
 * Map fra ordinær fastsatt dag til justert dato for helligdager. Se [Confluence](https://confluence.adeo.no/x/uizUL) for dokumentasjon.
 */
val helligdagsunntakFastsattMeldedag =
    mapOf(
        LocalDate.of(2025, 12, 22) to LocalDate.of(2025, 12, 17)
    )

/**
 * Sjekker om en meldeperiode inneholder et helligdagsunntak for å
 * kunne beregne meldekort for "fritak for meldeplikt"-brukere.
 */
fun erHelligdagsUnntak(dato: LocalDate): Boolean {
    val unntaksDatoer = helligdagsunntakFritaksUtbetalingDato.values
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