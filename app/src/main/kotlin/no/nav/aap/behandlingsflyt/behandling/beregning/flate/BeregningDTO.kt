package no.nav.aap.behandlingsflyt.behandling.beregning.flate

import java.math.BigDecimal

enum class BeregningstypeDTO {
    STANDARD,
    UFØRE,
    YRKESSKADE,
    YRKESSKADE_UFØRE
}

class BeregningDTO(
    val beregningstypeDTO: BeregningstypeDTO,
    val grunnlag: BigDecimal,
    val grunnlag11_19: Grunnlag11_19DTO? = null,
    val grunnlagYrkesskade: GrunnlagYrkesskadeDTO? = null,
    val grunnlagUføre: GrunnlagUføreDTO? = null,
    val grunnlagYrkesskadeUføre: GrunnlagYrkesskadeUføreDTO? = null,
)

class GrunnlagUføreDTO(
    val grunnlaget: BigDecimal,
    val type: String,
    val grunnlag: Grunnlag11_19DTO,
    val grunnlagYtterligereNedsatt: Grunnlag11_19DTO,
    val uføregrad: Int,
    val uføreInntekterFraForegåendeÅr: Map<String, BigDecimal>, //uføre ikke oppjustert
    val uføreInntektIKroner: BigDecimal, //grunnlaget
    val uføreYtterligereNedsattArbeidsevneÅr: Int,
    val er6GBegrenset: Boolean, //skal være individuelt på hver inntekt
    val erGjennomsnitt: Boolean,
)

class Grunnlag11_19DTO(
    val grunnlaget: BigDecimal,
    val er6GBegrenset: Boolean,
    val erGjennomsnitt: Boolean,
    val inntekter: Map<String, BigDecimal>,
)

class GrunnlagYrkesskadeDTO(
    val grunnlaget: BigDecimal,
    val beregningsgrunnlag: Grunnlag11_19DTO,
    val terskelverdiForYrkesskade: Int,
    val andelSomSkyldesYrkesskade: BigDecimal,
    val andelYrkesskade: Int,
    val benyttetAndelForYrkesskade: Int,
    val andelSomIkkeSkyldesYrkesskade: BigDecimal,
    val antattÅrligInntektYrkesskadeTidspunktet: BigDecimal,
    val yrkesskadeTidspunkt: Int,
    val grunnlagForBeregningAvYrkesskadeandel: BigDecimal,
    val yrkesskadeinntektIG: BigDecimal,
    val grunnlagEtterYrkesskadeFordel: BigDecimal,
    val er6GBegrenset: Boolean,
    val erGjennomsnitt: Boolean
)

class GrunnlagYrkesskadeUføreDTO(
    val grunnlaget: BigDecimal,
    val beregningsgrunnlag: GrunnlagUføreDTO,
    val terskelverdiForYrkesskade: Int,
    val andelSomSkyldesYrkesskade: BigDecimal,
    val andelYrkesskade: Int,
    val benyttetAndelForYrkesskade: Int,
    val andelSomIkkeSkyldesYrkesskade: BigDecimal,
    val antattÅrligInntektYrkesskadeTidspunktet: BigDecimal,
    val yrkesskadeTidspunkt: Int,
    val grunnlagForBeregningAvYrkesskadeandel: BigDecimal,
    val yrkesskadeinntektIG: BigDecimal,
    val grunnlagEtterYrkesskadeFordel: BigDecimal,
    val er6GBegrenset: Boolean,
    val erGjennomsnitt: Boolean
)

/*
class BeregningDTO(
    val beregningsGrunnlag: GUnit,
    val faktagrunnlag: Faktagrunnlag,
    val nedsattArbeidsevneÅr,
    val inntekterFraForegåendeÅr,
    val inntektIKroner,
    val inntektIG,
    val er6GBegrenset,
    val erDetBruktGjennomsnitt,

    val antattÅrligInntektYrkesskadetidspunkt,
    val yrkesskadetidspunkt,
    val er6GBegrenset,//????
    val TerskelverdiForYrkesskadefordel,
    val AndelYrkesskade,
    val BenyttetAndelYrkesskade,
    val InntektPåYrkesskadetidspunkt,
    val YrkesskadeinntektIG,
    val grunnlagForBeregningAvYrkesskadeandel,
    val andelSomSkyldesYrkesskade,
    val andelSomIkkeSkyldesYrkesskade,
    val grunnlagEtterYrkesskadefordel,

    val uføreYtterligereNedsattArbeidsevneÅr,
    val uføreInntekterFraForegåendeÅr,
    val uføregrad,//Liste? Avhengig av om vi skal se på grad over tid
    val uføreOppjusterteInntekter,
    val uføreInntektIKroner,
    val uføreInntektIG,
    val uføreEr6GBegrenset,
    val uføreErDetBruktGjennomsnitt,
    )
 */