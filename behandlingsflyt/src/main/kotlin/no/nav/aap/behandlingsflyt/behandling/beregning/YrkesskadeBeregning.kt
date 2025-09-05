package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.komponenter.verdityper.Prosent

/**
 * @param grunnlag11_19 Beregningsgrunnlag, muligens justert etter uføregrad.
 * @param antattÅrligInntekt Antatt årlig inntekt på yrkesskadetidspunktet.
 * @param andelAvNedsettelsenSomSkyldesYrkesskaden Selvforklarende.
 */
class YrkesskadeBeregning(
    private val grunnlag11_19: Beregningsgrunnlag,
    private val antattÅrligInntekt: InntektPerÅr,
    private val andelAvNedsettelsenSomSkyldesYrkesskaden: Prosent
) {
    private companion object {
        private val TERSKELVERDI_FOR_YRKESSKADE = Prosent.`70_PROSENT`
    }

    fun beregnYrkesskaden(): GrunnlagYrkesskade {
        val grunnlagFra11_19 = grunnlag11_19.grunnlaget()

        val benyttetGjennomsnittsbeløp = antattÅrligInntekt.gUnit()
        val antattÅrligInntektGUnits = benyttetGjennomsnittsbeløp.gUnit.begrensTil6GUnits()
        val grunnbeløp = benyttetGjennomsnittsbeløp.beløp

        // grunnlaget for arbeidsavklaringspengene skal ikke settes lavere enn den antatte årlige
        // arbeidsinntekten på skadetidspunktet:
        val grunnlagForBeregningAvYrkesskadeandel = maxOf(grunnlagFra11_19, antattÅrligInntektGUnits)

        // Om nedsettelsesgraden er større enn 70%, oppjusteres den til 100%.
        // (§11-22, andre ledd)
        val andelForBeregning = andelAvNedsettelsenSomSkyldesYrkesskaden.justertFor(TERSKELVERDI_FOR_YRKESSKADE)

        val andelSomSkyldesYrkesskade = grunnlagForBeregningAvYrkesskadeandel.multiplisert(andelForBeregning)
        val andelSomIkkeSkyldesYrkesskade = grunnlagFra11_19.multiplisert(andelForBeregning.komplement())

        val grunnlag = andelSomSkyldesYrkesskade.pluss(andelSomIkkeSkyldesYrkesskade)

        return GrunnlagYrkesskade(
            grunnlaget = grunnlag,
            beregningsgrunnlag = grunnlag11_19,
            terskelverdiForYrkesskade = TERSKELVERDI_FOR_YRKESSKADE,
            andelYrkesskade = andelAvNedsettelsenSomSkyldesYrkesskaden,
            benyttetAndelForYrkesskade = andelForBeregning,
            antattÅrligInntektYrkesskadeTidspunktet = antattÅrligInntekt.beløp,
            yrkesskadeTidspunkt = antattÅrligInntekt.år,
            grunnbeløp = grunnbeløp,
            yrkesskadeinntektIG = antattÅrligInntektGUnits,
            andelSomSkyldesYrkesskade = andelSomSkyldesYrkesskade,
            andelSomIkkeSkyldesYrkesskade = andelSomIkkeSkyldesYrkesskade,
            grunnlagEtterYrkesskadeFordel = grunnlag,
            grunnlagForBeregningAvYrkesskadeandel = grunnlagForBeregningAvYrkesskadeandel
        )
    }
}
