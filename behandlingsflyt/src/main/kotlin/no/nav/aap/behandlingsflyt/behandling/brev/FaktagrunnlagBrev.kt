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
    val beregningsmetode: Beregningsmetode? = null,
    val beregningstype: Beregningstype? = null,
    val uføreValgKategori: UføreValgKategori? = null,
    val yrkesskadeValgKategori: YrkesskadeValgKategori? = null,
    val beregningsutfallKategori: BeregningsutfallKategori? = null,
) {
    enum class Beregningsmetode { SISTE_ÅR, TREÅRS_GJENNOMSNITT }
    enum class Beregningstype { STANDARD, UFØRE, YRKESSKADE, YRKESSKADE_UFØRE }

    /** Kun satt for [Beregningstype.UFØRE] og [Beregningstype.YRKESSKADE_UFØRE]. Null ellers. */
    enum class UføreValgKategori {
        /** § 11-19: Beregningstidspunkt = uføretidspunktet. [GrunnlagUføre.Type.STANDARD]. */
        UFORETIDSPUNKT,
        /** § 11-28 fjerde ledd: Ytterligere nedsatt-tidspunktet ga høyere grunnlag. Én uføregrad-periode. */
        YTTERLIGERE_NEDSATT,
        /** § 11-28 fjerde ledd: Som [YTTERLIGERE_NEDSATT], men uføregrad ble økt underveis (flere perioder). */
        YTTERLIGERE_NEDSATT_OKT_UFOREGRAD,
    }

    /** Kun satt for [Beregningstype.YRKESSKADE] og [Beregningstype.YRKESSKADE_UFØRE]. Null ellers. */
    enum class YrkesskadeValgKategori {
        /** § 11-22: Ordinær beregning (§ 11-19) ga like mye eller mer. */
        STANDARD_VINNER,
        /** § 11-22 tredje ledd: Yrkesskadeandel > 70 % → sykepengegrunnlaget på skadetidspunktet (§ 8-55) brukes. */
        SYKEPENGEGRUNNLAG,
        /** § 11-22 andre ledd: Yrkesskadeberegningen vant med proporsjonal fordel (andel ≤ 70 %). */
        FORDEL_70_ELLER_MINDRE,
    }

    /** Alltid satt når beregningsgrunnlag foreligger. Resultat av § 11-16-beregningen. */
    enum class BeregningsutfallKategori {
        SISTE_AAR,
        GJENNOMSNITT,
        MINSTESATS_25_ELLER_MER,
        MINSTESATS_UNDER_25,
        INNTEKT_OVER_6G,
    }
}

data class InntektPerÅr(val år: Year, val inntekt: BigDecimal)

data class ForholdTilAndreYtelser(
    val fradragAndreYtelser: List<FradragYtelse>,
    val reduksjonArbeidsgiver: List<ReduksjonArbeidsgiver>,
    val refusjonskravTjenestepensjon: RefusjonskravTjenestepensjon?,
    val samordningAndreYtelser: List<SamordningYtelse>,
    val samordningBarnepensjon: List<SamordningBarnepensjon>,
    val samordningUføre: List<SamordningUføre>,
    val sykestipend: List<Sykestipend>,
)

data class SamordningYtelse(
    val ytelseNavn: String,
    val gradering: Int,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
)

data class SamordningUføre(
    val virkningstidspunkt: LocalDate,
    val uføregradProsent: Int,
)

data class ReduksjonArbeidsgiver(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
)

data class RefusjonskravTjenestepensjon(
    val skalEtterbetalingHoldesIgjen: Boolean,
    val fraOgMed: LocalDate?,
    val tilOgMed: LocalDate?,
)

data class Sykestipend(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
)

data class SamordningBarnepensjon(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val månedsats: BigDecimal,
)

data class FradragYtelse(
    val ytelseNavn: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
)

data class YrkesskadeBeregningBrev(
    val yrkesskader: List<Yrkesskade>,
    val andelAvNedsettelseSomSkyldesYrkesskade: Int?,
) {
    data class Yrkesskade(
        val yrkesskadedato: LocalDate,
        val arbeidsinntektPaaSkadetidspunktet: BigDecimal?,
    )
}
