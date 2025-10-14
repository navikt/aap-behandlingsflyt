package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.komponenter.verdityper.Beløp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year

data class TilkjentYtelse(
    val dagsats: Beløp,
    val gradertDagsats: Beløp,
    val barnetillegg: Beløp,
    val gradertBarnetillegg: Beløp,
    val gradertDagsatsInkludertBarnetillegg: Beløp,
    val antallBarn: Int,
    val barnetilleggsats: Beløp
)

data class GrunnlagBeregning(
    val beregningstidspunkt: LocalDate?,
    val inntekterPerÅr: List<InntektPerÅr>,
    val beregningsgrunnlag: Beløp?,
)

data class InntektPerÅr(val år: Year, val inntekt: BigDecimal)
