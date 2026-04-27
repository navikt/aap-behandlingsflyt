package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

data class TilkjentYtelse(
    val dagsats: Beløp,
    val gradertDagsats: Beløp,
    val barnetillegg: Beløp,
    val gradertBarnetillegg: Beløp,
    val gradertDagsatsInkludertBarnetillegg: Beløp,
    val antallBarn: Int,
    val barnetilleggsats: Beløp,
    val minsteÅrligYtelse: Beløp,
    val minsteÅrligYtelseUnder25: Beløp,
    val årligYtelse: Beløp,
    val kravdatoUføretrygd: LocalDate?
)

data class GrunnlagBeregning(
    val beregningstidspunkt: LocalDate?,
    val inntekterPerÅr: List<InntektPerÅr>,
    val beregningsgrunnlag: Beløp?,
)

data class InntektPerÅr(val år: Year, val inntekt: BigDecimal)

data class Samordning(
    val andreYtelser: SamordningAndreYtelser?,
    val uførePerioder: List<SamordningUførePeriode>?,
    val ytelseFraArbeidsgiver: SamordningYtelseFraArbeidsgiver?,
    val tjenestepensjon: SamordningTjenestepensjon?,
    val sykestipend: SamordningerSykestipend?,
    val barnepensjon: SamordningerBarnepensjon?,
    val fradragAndreYtelser: SamordningerFradragAndreYtelser?,
)

data class SamordningAndreYtelser(
    val perioder: List<SamordningAndreYtelserPeriode>,
) {
    data class SamordningAndreYtelserPeriode(
        val ytelseType: String,
        val periode: Periode,
        val gradering: Int?,
    )
}

data class SamordningUførePeriode(
    val virkningstidspunkt: LocalDate,
    val uføregradTilSamordning: Int,
)

data class SamordningYtelseFraArbeidsgiver(
    val perioder: List<Periode>,
)

data class SamordningTjenestepensjon(
    val harKrav: Boolean,
    val fom: LocalDate?,
    val tom: LocalDate?,
)

data class SamordningerSykestipend(
    val perioder: List<Periode>,
)

data class SamordningerBarnepensjon(
    val perioder: List<SamordningBarnepensjonPeriode>,
) {
    data class SamordningBarnepensjonPeriode(
        val fom: YearMonth,
        val tom: YearMonth?,
        val månedsats: Beløp,
    )
}

data class SamordningerFradragAndreYtelser(
    val perioder: List<SamordningFradragAnnenYtelsePeriode>,
) {
    data class SamordningFradragAnnenYtelsePeriode(
        val ytelse: String,
        val periode: Periode,
    )
}
