package no.nav.aap.behandlingsflyt.hendelse.statistikk

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import java.math.BigDecimal
import java.time.LocalDate

data class VilkårsResultatDTO(
    val typeBehandling: TypeBehandling,
    val vilkår: List<VilkårDTO>,
) {
    companion object {
        fun fraDomeneObjekt(
            typeBehandling: TypeBehandling,
            vilkårsresultat: Vilkårsresultat,
        ): VilkårsResultatDTO {
            return VilkårsResultatDTO(
                typeBehandling,
                vilkårsresultat.alle().map { VilkårDTO.fraDomeneObjekt(it) }
            )
        }
    }
}

data class VilkårDTO(val vilkårType: Vilkårtype, val perioder: List<VilkårsPeriodeDTO>) {
    companion object {
        fun fraDomeneObjekt(vilkår: Vilkår): VilkårDTO {
            return VilkårDTO(
                vilkårType = vilkår.type,
                perioder = vilkår.vilkårsperioder().map { VilkårsPeriodeDTO.fraDomeneObjekt(it) })
        }
    }
}

data class VilkårsPeriodeDTO(
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val utfall: Utfall,
    val manuellVurdering: Boolean,
    val innvilgelsesårsak: Innvilgelsesårsak?,
    val avslagsårsak: Avslagsårsak?
) {
    companion object {
        fun fraDomeneObjekt(vilkårsperiode: Vilkårsperiode): VilkårsPeriodeDTO {
            return VilkårsPeriodeDTO(
                fraDato = vilkårsperiode.periode.fom,
                tilDato = vilkårsperiode.periode.tom,
                utfall = vilkårsperiode.utfall,
                manuellVurdering = vilkårsperiode.manuellVurdering,
                innvilgelsesårsak = vilkårsperiode.innvilgelsesårsak,
                avslagsårsak = vilkårsperiode.avslagsårsak
            )
        }
    }
}

data class TilkjentYtelseDTO(val perioder: List<TilkjentYtelsePeriodeDTO>)

data class TilkjentYtelsePeriodeDTO(
    val dagsats: Number,
    val fraDato: LocalDate,
    val tilDato: LocalDate,
    val gradering: Number
)

data class AvsluttetBehandlingDTO(
    val behandlingsReferanse: BehandlingReferanse,
    val saksnummer: Saksnummer,
    val vilkårsResultat: VilkårsResultatDTO,
    val tilkjentYtelse: TilkjentYtelseDTO,
    val beregningsGrunnlag: BeregningsgrunnlagDTO
)

data class Grunnlag11_19DTO(
    val inntekter: Map<String, BigDecimal>,
)

data class GrunnlagYrkesskadeDTO(
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
)

/**
 * @property uføreInntekterFraForegåendeÅr Uføre ikke oppjustert
 * @property uføreInntektIKroner Grunnlaget
 */
data class GrunnlagUføreDTO(
    val type: String,
    val grunnlag: Grunnlag11_19DTO,
    val grunnlagYtterligereNedsatt: Grunnlag11_19DTO,
    val uføregrad: Int,
    val uføreInntekterFraForegåendeÅr: Map<String, BigDecimal>,
    val uføreInntektIKroner: BigDecimal,
    val uføreYtterligereNedsattArbeidsevneÅr: Int,
)

/**
 * Felter fra BeregningsGrunnlag-interfacet ([no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag]),
 * er alltid med. Minst én av grunnlag11_19dto, grunnlagYrkesskade, grunnlagUføre er ikke-null.
 */
data class BeregningsgrunnlagDTO(
    val grunnlag: Double,
    val grunnlag11_19dto: Grunnlag11_19DTO? = null,
    val grunnlagYrkesskade: GrunnlagYrkesskadeDTO? = null,
    val grunnlagUføre: GrunnlagUføreDTO? = null
) {
    init {
        require(grunnlag11_19dto != null || grunnlagYrkesskade != null || grunnlagUføre != null)
    }
}
