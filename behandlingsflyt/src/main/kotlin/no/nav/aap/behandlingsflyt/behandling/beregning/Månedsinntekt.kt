package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.komponenter.verdityper.Beløp
import java.time.YearMonth

data class Månedsinntekt(
    val årMåned: YearMonth,
    val beløp: Beløp
)

