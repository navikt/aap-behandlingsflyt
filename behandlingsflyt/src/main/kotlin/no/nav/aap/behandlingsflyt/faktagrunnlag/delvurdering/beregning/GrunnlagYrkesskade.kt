package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
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
data class GrunnlagYrkesskade(
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
}
