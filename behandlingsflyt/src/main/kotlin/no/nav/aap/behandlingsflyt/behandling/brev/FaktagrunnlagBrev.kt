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
    val beregningsutfallKategori: BeregningsutfallKategori? = null,
) {
    /** Resultat av § 11-19-beregningen. Null kun når beregningsgrunnlag er null (manglende dato, f.eks. VurderesForUføretrygd). */
    enum class BeregningsutfallKategori {
        SISTE_AAR,
        GJENNOMSNITT,
        MINSTESATS_OVER_25,
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
        val relevantForArbeidsevne: Boolean,
        val diagnose: String?,
    )
}
