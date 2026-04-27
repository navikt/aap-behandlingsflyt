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

data class SamordningFaktagrunnlag(
    val andreYtelser: SamordningAndreYtelserFaktagrunnlag?,
    val uførePerioder: List<SamordningUførePeriodeFaktagrunnlag>?,
    val ytelseFraArbeidsgiver: SamordningYtelseFraArbeidsgiverFaktagrunnlag?,
    val tjenestepensjonRefusjonskrav: TjenestepensjonRefusjonskravFaktagrunnlag?,
    val sykestipend: SykestipendFaktagrunnlag?,
    val barnepensjon: BarnepensjonFaktagrunnlag?,
    val andreStatligeYtelser: AndreStatligeYtelserFaktagrunnlag?,
)

data class SamordningAndreYtelserFaktagrunnlag(
    val perioder: List<SamordningAndreYtelserPeriodeFaktagrunnlag>,
) {
    data class SamordningAndreYtelserPeriodeFaktagrunnlag(
        val ytelseType: String,
        val periode: Periode,
        val gradering: Int?,
    )
}

data class SamordningUførePeriodeFaktagrunnlag(
    val virkningstidspunkt: LocalDate,
    val uføregradTilSamordning: Int,
)

data class SamordningYtelseFraArbeidsgiverFaktagrunnlag(
    val perioder: List<Periode>,
)

data class TjenestepensjonRefusjonskravFaktagrunnlag(
    val harKrav: Boolean,
    val fom: LocalDate?,
    val tom: LocalDate?,
)

data class SykestipendFaktagrunnlag(
    val perioder: List<Periode>,
)

data class BarnepensjonFaktagrunnlag(
    val perioder: List<BarnepensjonPeriodeFaktagrunnlag>,
) {
    data class BarnepensjonPeriodeFaktagrunnlag(
        val fom: YearMonth,
        val tom: YearMonth?,
        val månedsats: Beløp,
    )
}

data class AndreStatligeYtelserFaktagrunnlag(
    val perioder: List<AndreStatligeYtelserPeriodeFaktagrunnlag>,
) {
    data class AndreStatligeYtelserPeriodeFaktagrunnlag(
        val ytelse: String,
        val periode: Periode,
    )
}
