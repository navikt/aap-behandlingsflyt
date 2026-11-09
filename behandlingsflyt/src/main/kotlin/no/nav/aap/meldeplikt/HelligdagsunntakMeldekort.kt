package no.nav.aap.meldeplikt

import no.nav.aap.komponenter.miljo.Miljø
import java.time.LocalDate
import kotlin.collections.mapOf

/**
 * Map fra ordinær fastsatt dag til justert dato for helligdager. Se [Confluence](https://confluence.adeo.no/x/uizUL) for dokumentasjon.
 */

val EKSISTERENDE_MELDEDAGUNNTAK_I_PROD = mapOf(
    LocalDate.of(2025, 12, 22) to LocalDate.of(2025, 12, 17)
)

val TEST_MELDEDAGUNNTAK_I_DEV = mapOf(
    LocalDate.of(2026, 11, 30) to LocalDate.of(2026, 11, 25),
    LocalDate.of(2026, 12, 7) to LocalDate.of(2026, 12, 2)
)

val KOMMENDE_MELDEDAGUNNTAK_I_PROD = mapOf(
    LocalDate.of(2026, 12, 21) to LocalDate.of(2026, 12, 16),
)

fun helligdagsunntakFastsattMeldedag(erProd: Boolean = Miljø.erProd()): Map<LocalDate, LocalDate> =
    EKSISTERENDE_MELDEDAGUNNTAK_I_PROD + if (!erProd) (TEST_MELDEDAGUNNTAK_I_DEV + KOMMENDE_MELDEDAGUNNTAK_I_PROD) else emptyMap()

val eksisterndeFritaksUnntakIProd = mapOf(
    LocalDate.of(2025, 12, 17) to LocalDate.of(2025, 12, 15),
    LocalDate.of(2025, 12, 24) to LocalDate.of(2025, 12, 22),
    LocalDate.of(2025, 12, 31) to LocalDate.of(2025, 12, 29),
    LocalDate.of(2026, 4, 1) to LocalDate.of(2026, 3, 30),
    LocalDate.of(2026, 5, 13) to LocalDate.of(2026, 5, 12)
)

val testFritaksUnntakIDev = mapOf(
    LocalDate.of(2026, 11, 25) to LocalDate.of(2026, 11, 23),
    LocalDate.of(2026, 12, 2) to LocalDate.of(2026, 11, 30),
    LocalDate.of(2026, 12, 9) to LocalDate.of(2026, 12, 7),
)

val kommendeFritaksUnntakIProd = mapOf(
    LocalDate.of(2026, 12, 16) to LocalDate.of(2026, 12, 14),
    LocalDate.of(2026, 12, 23) to LocalDate.of(2026, 12, 21),
    LocalDate.of(2026, 12, 30) to LocalDate.of(2026, 12, 28),
)

fun helligdagsunntakFritaksUtbetalingDato(erProd: Boolean = Miljø.erProd()): Map<LocalDate, LocalDate> =
    eksisterndeFritaksUnntakIProd + if (!erProd) testFritaksUnntakIDev + kommendeFritaksUnntakIProd else emptyMap()

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