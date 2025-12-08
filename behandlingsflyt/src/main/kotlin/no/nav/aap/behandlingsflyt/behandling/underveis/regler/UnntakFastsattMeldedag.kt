package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.miljo.Miljø
import java.time.LocalDate

/**
 * Map fra ordinær fastsatt dag til justert dato for helligdager. Se [Confluence](https://confluence.adeo.no/x/uizUL) for dokumentasjon.
 *
 * Merk: if-else er for å kunne teste tidligere i dev.
 */
val unntakFastsattMeldedag = if (Miljø.erProd()) {
    mapOf(
        LocalDate.of(2025, 12, 22) to LocalDate.of(2025, 12, 17),
        LocalDate.of(2026, 3, 30) to LocalDate.of(2026, 3, 25)
    )
} else mapOf(
    LocalDate.of(2025, 12, 22).minusDays(14) to LocalDate.of(2025, 12, 17).minusDays(14),
    LocalDate.of(2026, 3, 30).minusDays(14) to LocalDate.of(2026, 3, 25).minusDays(14)
)

val unntakFritaksUtbetalingDato = if (Miljø.erProd()) {
    mapOf(
        LocalDate.of(2025, 12, 17) to LocalDate.of(2025, 12, 15),
        LocalDate.of(2025, 12, 24) to LocalDate.of(2025, 12, 22),
        LocalDate.of(2025, 12, 31) to LocalDate.of(2025, 12, 29),
        LocalDate.of(2026, 4, 1) to LocalDate.of(2025, 3, 30),
    )
} else mapOf(
    LocalDate.of(2025, 12, 22).minusDays(14) to LocalDate.of(2025, 12, 17).minusDays(14),
    LocalDate.of(2026, 3, 30).minusDays(14) to LocalDate.of(2026, 3, 25).minusDays(14)
)

