package no.nav.aap.tilkjentytelse

import java.time.LocalDate
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Tid

val BARNETILLEGGSATS_TIDSLINJE = Tidslinje(
    listOf(
        Segment(
            periode = Periode(LocalDate.MIN, LocalDate.of(2023, 1, 31)),
            verdi = Beløp(27)
        ),
        Segment(
            periode = Periode(LocalDate.of(2023, 2, 1), LocalDate.of(2023, 12, 31)),
            verdi = Beløp(35)
        ),
        Segment(
            periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)),
            verdi = Beløp(36)
        ),
        Segment(
            periode = Periode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)),
            verdi = Beløp(37)
        ),
        Segment(
            periode = Periode(LocalDate.of(2026, 1, 1), Tid.MAKS),
            verdi = Beløp(38)
        )
    )
)