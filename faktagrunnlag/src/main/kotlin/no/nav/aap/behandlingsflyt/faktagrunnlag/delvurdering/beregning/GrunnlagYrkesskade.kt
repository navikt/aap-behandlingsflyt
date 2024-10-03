package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Faktagrunnlag
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Prosent
import java.math.BigDecimal
import java.time.Year

/**
 * @param grunnlaget Det beregnede grunnlaget gitt yrkesskade.
 * @param beregningsgrunnlag Hvilket grunnlag som ble brukt i beregningen for yrkesskade, muligens justert for uføre.
 * @param terskelverdiForYrkesskade Gjeldende terskelverdi for yrkesskade (definert i §11-22).
 * @param andelSomSkyldesYrkesskade Hvor stor del av [grunnlaget] som kommer fra yrkesskade.
 * @param andelYrkesskade Yrkesskadeprosent.
 * @param benyttetAndelForYrkesskade Yrkesskadeprosent, muligens oppjustert etter [terskelverdiForYrkesskade].
 * @param andelSomIkkeSkyldesYrkesskade Hvor stor del av [grunnlaget] som ikke kommer fra yrkesskade.
 * @param antattÅrligInntektYrkesskadeTidspunktet Selvforklarende.
 * @param yrkesskadeTidspunkt Hvilket år yrkesskaden skjedde.
 * @param grunnlagForBeregningAvYrkesskadeandel Delen av [grunnlaget] som skyldes yrkesskade.
 * @param grunnbeløp Grunnbeløp i året for yrkesskadetidspunktet.
 * @param yrkesskadeinntektIG Inntekt på yrkesskadetidspunktet i G.
 * @param grunnlagEtterYrkesskadeFordel Dette er det største av grunnlagene etter §11-19 eller inntekt på yrkesskadetidspunktet.
 */
class GrunnlagYrkesskade(
    private val grunnlaget: GUnit,
    private val beregningsgrunnlag: Beregningsgrunnlag,
    private val terskelverdiForYrkesskade: Prosent,
    private val andelSomSkyldesYrkesskade: GUnit,
    private val andelYrkesskade: Prosent,
    private val benyttetAndelForYrkesskade: Prosent,
    private val andelSomIkkeSkyldesYrkesskade: GUnit,
    private val antattÅrligInntektYrkesskadeTidspunktet: Beløp,
    private val yrkesskadeTidspunkt: Year,
    private val grunnlagForBeregningAvYrkesskadeandel: GUnit,
    private val grunnbeløp: Beløp,
    private val yrkesskadeinntektIG: GUnit,
    private val grunnlagEtterYrkesskadeFordel: GUnit
) : Beregningsgrunnlag {

    override fun grunnlaget(): GUnit {
        return grunnlaget
    }

    fun terskelverdiForYrkesskade(): Prosent {
        return terskelverdiForYrkesskade
    }

    fun andelYrkesskade(): Prosent {
        return andelYrkesskade
    }

    fun andelSomSkyldesYrkesskade(): GUnit {
        return andelSomSkyldesYrkesskade
    }

    fun andelSomIkkeSkyldesYrkesskade(): GUnit {
        return andelSomIkkeSkyldesYrkesskade
    }

    fun benyttetAndelForYrkesskade(): Prosent {
        return benyttetAndelForYrkesskade
    }

    fun yrkesskadeTidspunkt(): Year {
        return yrkesskadeTidspunkt
    }

    fun grunnbeløp(): Beløp {
        return grunnbeløp
    }

    fun yrkesskadeinntektIG(): GUnit {
        return yrkesskadeinntektIG
    }

    fun antattÅrligInntektYrkesskadeTidspunktet(): Beløp {
        return antattÅrligInntektYrkesskadeTidspunktet
    }

    fun grunnlagForBeregningAvYrkesskadeandel(): GUnit {
        return grunnlagForBeregningAvYrkesskadeandel
    }

    fun grunnlagEtterYrkesskadeFordel(): GUnit {
        return grunnlagEtterYrkesskadeFordel
    }

    override fun faktagrunnlag(): Faktagrunnlag {
        return Fakta(
            grunnlaget = grunnlaget.verdi(),
            beregningsgrunnlag = beregningsgrunnlag.faktagrunnlag()
        )
    }

    internal class Fakta(
        // FIXME: BigDecimal serialiseres til JSON på standardform
        val grunnlaget: BigDecimal,
        val beregningsgrunnlag: Faktagrunnlag
    ) : Faktagrunnlag

    fun underliggende(): Beregningsgrunnlag {
        return beregningsgrunnlag
    }

    override fun toString(): String {
        return "GrunnlagYrkesskade(grunnlaget=$grunnlaget, beregningsgrunnlag=$beregningsgrunnlag)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GrunnlagYrkesskade

        if (grunnlaget != other.grunnlaget) return false
        if (beregningsgrunnlag != other.beregningsgrunnlag) return false
        if (terskelverdiForYrkesskade != other.terskelverdiForYrkesskade) return false
        if (andelSomSkyldesYrkesskade != other.andelSomSkyldesYrkesskade) return false
        if (andelYrkesskade != other.andelYrkesskade) return false
        if (benyttetAndelForYrkesskade != other.benyttetAndelForYrkesskade) return false
        if (andelSomIkkeSkyldesYrkesskade != other.andelSomIkkeSkyldesYrkesskade) return false
        if (antattÅrligInntektYrkesskadeTidspunktet != other.antattÅrligInntektYrkesskadeTidspunktet) return false
        if (yrkesskadeTidspunkt != other.yrkesskadeTidspunkt) return false
        if (grunnlagForBeregningAvYrkesskadeandel != other.grunnlagForBeregningAvYrkesskadeandel) return false
        if (grunnbeløp != other.grunnbeløp) return false
        if (yrkesskadeinntektIG != other.yrkesskadeinntektIG) return false
        if (grunnlagEtterYrkesskadeFordel != other.grunnlagEtterYrkesskadeFordel) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grunnlaget.hashCode()
        result = 31 * result + beregningsgrunnlag.hashCode()
        result = 31 * result + terskelverdiForYrkesskade.hashCode()
        result = 31 * result + andelSomSkyldesYrkesskade.hashCode()
        result = 31 * result + andelYrkesskade.hashCode()
        result = 31 * result + benyttetAndelForYrkesskade.hashCode()
        result = 31 * result + andelSomIkkeSkyldesYrkesskade.hashCode()
        result = 31 * result + antattÅrligInntektYrkesskadeTidspunktet.hashCode()
        result = 31 * result + yrkesskadeTidspunkt.hashCode()
        result = 31 * result + grunnlagForBeregningAvYrkesskadeandel.hashCode()
        result = 31 * result + grunnbeløp.hashCode()
        result = 31 * result + yrkesskadeinntektIG.hashCode()
        result = 31 * result + grunnlagEtterYrkesskadeFordel.hashCode()
        return result
    }
}
