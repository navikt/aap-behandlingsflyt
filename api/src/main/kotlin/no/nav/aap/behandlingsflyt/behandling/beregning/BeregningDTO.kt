package no.nav.aap.behandlingsflyt.behandling.beregning

import java.math.BigDecimal
import java.time.LocalDate

enum class BeregningstypeDTO {
    STANDARD,
    UFØRE,
    YRKESSKADE,
    YRKESSKADE_UFØRE
}

data class BeregningDTO(
    val beregningstypeDTO: BeregningstypeDTO,
    val grunnlag11_19: Grunnlag11_19DTO? = null,
    val grunnlagYrkesskade: YrkesskadeGrunnlagDTO? = null,
    val grunnlagUføre: UføreGrunnlagDTO? = null,
    val grunnlagYrkesskadeUføre: YrkesskadeUføreGrunnlagDTO? = null,
    val gjeldendeGrunnbeløp: GjeldendeGrunnbeløpDTO,
)

data class GjeldendeGrunnbeløpDTO(
    val grunnbeløp: BigDecimal,
    val dato: LocalDate,
)

data class Grunnlag11_19DTO(
    val nedsattArbeidsevneÅr: String,
    val inntekter: List<InntektDTO>,
    val gjennomsnittligInntektSiste3år: BigDecimal,
    val inntektSisteÅr: InntektDTO,
    val grunnlag: BigDecimal
)

data class InntektDTO(
    val år: String,
    val inntektIKroner: BigDecimal,
    val inntektIG: BigDecimal,
    val justertTilMaks6G: BigDecimal
)

data class YrkesskadeGrunnlagDTO(
    val inntekter: List<InntektDTO>,
    val yrkesskadeinntekt: YrkesskadeInntektDTO,
    val standardBeregning: StandardBeregningDTO,
    val standardYrkesskade: StandardYrkesskadeDTO,
    val gjennomsnittligInntektSiste3år: BigDecimal,
    val inntektSisteÅr: InntektDTO,
    val nedsattArbeidsevneÅr: String,
    val yrkesskadeTidspunkt: String,
    val yrkesskadeGrunnlag: BigDecimal,
    val grunnlag: BigDecimal
)

data class YrkesskadeInntektDTO(
    val prosentVekting: Int,
    val antattÅrligInntektIKronerYrkesskadeTidspunktet: BigDecimal,
    val andelGangerInntekt: BigDecimal,
    val andelGangerInntektIG: BigDecimal,
    val antattÅrligInntektIGYrkesskadeTidspunktet: BigDecimal,
    val justertTilMaks6G: BigDecimal
)

data class StandardBeregningDTO(
    val prosentVekting: Int,
    val inntektIG: BigDecimal,
    val andelGangerInntekt: BigDecimal,
    val andelGangerInntektIG: BigDecimal,
)

data class StandardYrkesskadeDTO(
    val prosentVekting: Int,
    val inntektIG: BigDecimal,
    val andelGangerInntekt: BigDecimal,
    val andelGangerInntektIG: BigDecimal,
)


data class UføreGrunnlagDTO(
    val nedsattArbeidsevneÅr: String,
    val ytterligereNedsattArbeidsevneÅr: String,
    val inntekter: List<InntektDTO>,
    val gjennomsnittligInntektSiste3år: BigDecimal,
    val inntektSisteÅr: InntektDTO,
    val uføreInntekter: List<UføreInntektDTO>,
    val gjennomsnittligInntektSiste3årUfør: BigDecimal,
    val inntektSisteÅrUfør: UføreInntektDTO,
    val grunnlag: BigDecimal
)

data class UføreInntektDTO(
    val år: String,
    val inntektIKroner: BigDecimal,
    val inntektIG: BigDecimal,
    val justertTilMaks6G: BigDecimal, // Denne er feil
    val justertForUføreGrad: BigDecimal,
    val justertForUføreGradiG: BigDecimal, //samme som over bare i g¢
    val uføreGrad: Int
)

data class YrkesskadeUføreGrunnlagDTO(
    val uføreGrunnlag: UføreGrunnlagDTO,
    val yrkesskadeGrunnlag: YrkesskadeGrunnlagDTO,
    val grunnlag: BigDecimal
)
