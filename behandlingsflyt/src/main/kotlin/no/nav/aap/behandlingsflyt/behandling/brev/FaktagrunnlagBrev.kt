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
    val samordninger: List<SamordningAnnenYtelse>,
) {
    data class SamordningAnnenYtelse(
        val ytelseNavn: String,
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
        val gradering: BigDecimal?,
    )
}

data class SamordningUførePeriode(
    val virkningstidspunkt: LocalDate,
    val uføregradTilSamordning: BigDecimal,
)

data class SamordningYtelseFraArbeidsgiver(
    val samordninger: List<SamordningArbeidsgiver>,
) {
    data class SamordningArbeidsgiver(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
    )
}

data class SamordningTjenestepensjon(
    val skalEtterbetalingHoldesIgjen: Boolean,
    val fraOgMed: LocalDate?,
    val tilOgMed: LocalDate?,
)

data class SamordningerSykestipend(
    val samordninger: List<SamordningSykestipend>,
) {
    data class SamordningSykestipend(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
    )
}

data class SamordningerBarnepensjon(
    val samordninger: List<SamordningBarnepensjon>,
) {
    data class SamordningBarnepensjon(
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate?,
        val månedsats: BigDecimal,
    )
}

data class SamordningerFradragAndreYtelser(
    val perioder: List<SamordningFradragAnnenYtelse>,
) {
    data class SamordningFradragAnnenYtelse(
        val ytelseNavn: String,
        val fraOgMed: LocalDate,
        val tilOgMed: LocalDate,
    )
}
