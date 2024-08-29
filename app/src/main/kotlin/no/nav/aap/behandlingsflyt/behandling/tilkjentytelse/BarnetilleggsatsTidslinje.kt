package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Beløp
import java.time.LocalDate

val BARNETILLEGGSATS_TIDSLINJE = Tidslinje(
    listOf(
        Segment(
            periode = Periode(LocalDate.MIN, LocalDate.of(2023, 1,31)),
            verdi = Beløp(27)
        ),
        Segment(
            periode = Periode(LocalDate.of(2023,2,1), LocalDate.of(2023,12,31)),
            verdi = Beløp(35)
        ),
        Segment(
            periode = Periode(LocalDate.of(2024,1,1), LocalDate.MAX),
            verdi = Beløp(36)
        )
    )
)