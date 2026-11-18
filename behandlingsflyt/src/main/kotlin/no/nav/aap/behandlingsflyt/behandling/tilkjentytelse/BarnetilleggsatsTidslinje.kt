package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate



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

val BARNETILLEGGSATS_TIDSLINJE_TEST = Tidslinje(
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
            periode = Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
            verdi = Beløp(38)
        ),
        Segment(
            periode = Periode(LocalDate.of(2027, 1, 1), Tid.MAKS),
            verdi = Beløp(39)
        )
    )
)

fun aktivBarnetilleggsatsTidslinje(): Tidslinje<Beløp> =
    if (Miljø.erProd()) BARNETILLEGGSATS_TIDSLINJE else BARNETILLEGGSATS_TIDSLINJE_TEST
