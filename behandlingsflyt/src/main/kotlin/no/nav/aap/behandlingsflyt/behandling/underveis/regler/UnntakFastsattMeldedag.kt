package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import java.time.LocalDate

/**
 * Map fra ordinær fastsatt dag til justert dato for helligdager. Se [Confluence](https://confluence.adeo.no/x/uizUL) for dokumentasjon.
 */
val unntakFastsattMeldedag = mapOf(
    LocalDate.of(2025, 12, 22) to LocalDate.of(2025, 12, 17),
    LocalDate.of(2026, 3, 30) to LocalDate.of(2026, 3, 25)
)

val unntakFritaksUtbetalingDato = mapOf(
    LocalDate.of(2025, 12, 17) to LocalDate.of(2025, 12, 15),
    LocalDate.of(2025, 12, 25) to LocalDate.of(2025, 12, 22),
    LocalDate.of(2025, 12, 31) to LocalDate.of(2025, 12, 29),
    LocalDate.of(2026, 4, 1) to LocalDate.of(2025, 3, 30),
)