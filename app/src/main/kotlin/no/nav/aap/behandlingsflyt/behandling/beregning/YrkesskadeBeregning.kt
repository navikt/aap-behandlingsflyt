package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.verdityper.Prosent

/**
 * @param grunnlag11_19 Beregningsgrunnlag, muligens justert etter uføregrad.
 * @param antattÅrligInntekt Antatt årlig inntekt på yrkesskadetidspunktet.
 * @param andelAvNedsettelsenSomSkyldesYrkesskaden Selvforklarende.
 */
class YrkesskadeBeregning(
    private val grunnlag11_19: Beregningsgrunnlag,
    // TODO: Skal antattÅrligInntekt begrenses til 6G i det hele tatt?...
    private val antattÅrligInntekt: InntektPerÅr,
    private val andelAvNedsettelsenSomSkyldesYrkesskaden: Prosent
) {
    private companion object {
        private val TERSKELVERDI_FOR_YRKESSKADE = Prosent.`70_PROSENT`
    }

    fun beregnYrkesskaden(): GrunnlagYrkesskade {
        // Om nedsettelsesgraden er større enn eller lik 70%, oppjusteres den til 100%.
        // (§11-22, andre ledd)
        val andelForBeregning = andelAvNedsettelsenSomSkyldesYrkesskaden.justertFor(TERSKELVERDI_FOR_YRKESSKADE)

        val grunnlagFra11_19 = grunnlag11_19.grunnlaget()
        // TODO: ...og skal antattÅrligInntektGUnits begrenses til 6G...
        val antattÅrligInntektGUnits = antattÅrligInntekt.gUnit()

        // grunnlaget for arbeidsavklaringspengene skal ikke settes lavere enn den antatte årlige
        // arbeidsinntekten på skadetidspunktet:
        val grunnlagForBeregningAvYrkesskadeandel = maxOf(grunnlagFra11_19, antattÅrligInntektGUnits)

        // TODO: ...eller skal andelSomSkyldesYrkesskade begrenses til 6G
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
            yrkesskadeinntektIG = antattÅrligInntektGUnits,
            andelSomSkyldesYrkesskade = andelSomSkyldesYrkesskade,
            andelSomIkkeSkyldesYrkesskade = andelSomIkkeSkyldesYrkesskade,
            grunnlagEtterYrkesskadeFordel = grunnlag,
            grunnlagForBeregningAvYrkesskadeandel = grunnlagForBeregningAvYrkesskadeandel,
            er6GBegrenset = grunnlag11_19.er6GBegrenset(),
            erGjennomsnitt = grunnlag11_19.erGjennomsnitt()
        )
    }
}
