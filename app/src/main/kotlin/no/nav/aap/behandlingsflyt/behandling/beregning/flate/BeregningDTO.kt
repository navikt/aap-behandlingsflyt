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
    val grunnlag11_19: Grunnlag11_19DTO? = null,
    val grunnlagYrkesskade: YrkesskadeGrunnlagDTO? = null,
    val grunnlagUføre: UføreGrunnlagDTO? = null,
    val grunnlagYrkesskadeUføre: YrkesskadeUføreGrunnlagDTO? = null,
)

class Grunnlag11_19DTO(
    val inntekter: List<InntektDTO>,
    val gjennomsnittligInntektSiste3år: BigDecimal,
    val inntektSisteÅr: InntektDTO,
    val grunnlag: BigDecimal
)

class InntektDTO(
    val år: String,
    val inntektIKroner: BigDecimal,
    val inntektIG: BigDecimal,
    val justertTilMaks6G: BigDecimal
)

class YrkesskadeGrunnlagDTO(
    val inntekter: List<InntektDTO>,
    val yrkesskadeinntekt: YrkesskadeInntektDTO,
    val standardBeregning: StandardBeregningDTO,
    val gjennomsnittligInntektSiste3år: BigDecimal,
    val inntektSisteÅr: InntektDTO,
    val yrkesskadeGrunnlag: BigDecimal,
    val grunnlag: BigDecimal
)

class YrkesskadeInntektDTO(
    val prosentVekting: Int,
    val antattÅrligInntektIKronerYrkesskadeTidspunktet: BigDecimal,
    val antattÅrligInntektIGYrkesskadeTidspunktet: BigDecimal,
    val justertTilMaks6G: BigDecimal
)

class StandardBeregningDTO(
    val prosentVekting: Int,
    val inntektIG: BigDecimal,
    val justertTilMaks6G: BigDecimal// Boolean?
)

class UføreGrunnlagDTO(
    val inntekter: List<InntektDTO>,
    val gjennomsnittligInntektSiste3år: BigDecimal,
    val inntektSisteÅr: InntektDTO,
    val uføreInntekter: List<UføreInntektDTO>,
    val gjennomsnittligInntektSiste3årUfør: BigDecimal,
    val inntektSisteÅrUfør: UføreInntektDTO,
    val grunnlag: BigDecimal
)

class UføreInntektDTO(
    val år: String,
    val inntektIKroner: BigDecimal,
    val inntektIG: BigDecimal,
    val justertTilMaks6G: BigDecimal,
    val justertForUføreGrad: BigDecimal,
    val uføreGrad: Int
)

class YrkesskadeUføreGrunnlagDTO(
    val uføreGrunnlag: UføreGrunnlagDTO,
    val yrkesskadeGrunnlag: YrkesskadeGrunnlagDTO
)
