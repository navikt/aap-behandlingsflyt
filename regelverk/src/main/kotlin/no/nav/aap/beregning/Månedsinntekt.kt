package no.nav.aap.beregning

import java.time.YearMonth
import no.nav.aap.komponenter.verdityper.Beløp

data class Månedsinntekt(
    val årMåned: YearMonth,
    val beløp: Beløp
)