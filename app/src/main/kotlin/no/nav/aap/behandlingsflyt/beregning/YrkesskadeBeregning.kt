package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.verdityper.Prosent

class YrkesskadeBeregning(
    private val grunnlag11_19: Beregningsgrunnlag,
    //TODO: Skal antattÅrligInntekt begrenses til 6G i det hele tatt?...
    private val antattÅrligInntekt: InntektPerÅr,
    private val andelAvNedsettelsenSomSkyldesYrkesskaden: Prosent
) {
    private companion object {
        private val TERSKELVERDI_FOR_YRKESSKADE = Prosent.`70_PROSENT`
    }

    fun beregnYrkesskaden(): GrunnlagYrkesskade {
        val andelForBeregning = andelAvNedsettelsenSomSkyldesYrkesskaden.justertFor(TERSKELVERDI_FOR_YRKESSKADE)
        val grunnlagFra11_19 = grunnlag11_19.grunnlaget()
        //TODO: ...og skal antattÅrligInntektGUnits begrenses til 6G...
        val antattÅrligInntektGUnits = antattÅrligInntekt.gUnit()

        val grunnlagForBeregningAvYrkesskadeandel = maxOf(grunnlagFra11_19, antattÅrligInntektGUnits)
        //TODO: ...eller skal andelSomSkyldesYrkesskade begrenses til 6G
        val andelSomSkyldesYrkesskade = grunnlagForBeregningAvYrkesskadeandel.multiplisert(andelForBeregning)
        val andelSomIkkeSkyldesYrkesskade = grunnlagFra11_19.multiplisert(andelForBeregning.kompliment())

        val grunnlag = andelSomSkyldesYrkesskade.pluss(andelSomIkkeSkyldesYrkesskade)

        return GrunnlagYrkesskade(
            grunnlaget = grunnlag,
            beregningsgrunnlag = grunnlag11_19,
            terskelverdiForYrkesskade = TERSKELVERDI_FOR_YRKESSKADE,
            andelYrkesskade = andelForBeregning,
            benyttetAndelForYrkesskade = andelAvNedsettelsenSomSkyldesYrkesskaden,
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
