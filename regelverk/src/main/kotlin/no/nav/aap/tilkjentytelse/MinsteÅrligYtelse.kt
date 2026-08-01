package no.nav.aap.tilkjentytelse

import java.time.LocalDate
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Tid

val MINSTE_ÅRLIG_YTELSE_TIDSLINJE = Tidslinje(
    listOf(
        Segment(
            periode = Periode(LocalDate.MIN, LocalDate.of(2024, 6, 30)),
            verdi = GUnit("2")
        ),
        Segment(
            periode = Periode(LocalDate.of(2024, 7, 1), Tid.MAKS),
            verdi = GUnit("2.041")
        )
    )
)