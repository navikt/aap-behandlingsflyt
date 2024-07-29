package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import java.time.Year

class Beregning(
    private val input: Inntektsbehov
) {
    internal fun beregneMedInput(): Beregningsgrunnlag {
        val grunnlag11_19 = beregn(input.utledForOrdinær())//6G begrensning ligger her samt gjennomsnitt

        val beregningMedEllerUtenUføre = if (input.skalBeregneMedUføre()) {
            val ikkeOppjusterteInntekter = input.utledForYtterligereNedsatt()
            val oppjusterteInntekter = ikkeOppjusterteInntekter.map {
                InntektPerÅr(it.år, it.beløp.dividert(input.uføregrad().komplement()))
            }
            // år kommer herfra //6G begrensning ligger her samt gjennomsnitt
            val beregningVedUføre = beregn(oppjusterteInntekter.toSet())
            val uføreberegning = UføreBeregning(
                grunnlag = grunnlag11_19,
                ytterligereNedsattGrunnlag = beregningVedUføre,
                //TODO:
                // Hva hvis bruker har flere uføregrader?
                // Skal saksbahandler velge den som er knyttet til ytterligere nedsatt-tidspunktet?
                uføregrad = input.uføregrad(),
                inntekterForegåendeÅr = ikkeOppjusterteInntekter
            )
            val grunnlagUføre = uføreberegning.beregnUføre(Year.from(input.hentYtterligereNedsattArbeidsevneDato()))
            grunnlagUføre
        } else {
            grunnlag11_19
        }

        val beregningMedEllerUtenUføreMedEllerUtenYrkesskade =
            if (input.skalBeregneMedYrkesskadeFordel()) { //11-22
                val inntektPerÅr = InntektPerÅr(
                    Year.from(input.skadetidspunkt()),
                    input.antattÅrligInntekt()
                )
                val yrkesskaden = YrkesskadeBeregning(
                    grunnlag11_19 = beregningMedEllerUtenUføre,
                    antattÅrligInntekt = inntektPerÅr,
                    andelAvNedsettelsenSomSkyldesYrkesskaden = input.andelYrkesskade()
                ).beregnYrkesskaden()
                yrkesskaden
            } else {
                beregningMedEllerUtenUføre
            }
        return beregningMedEllerUtenUføreMedEllerUtenYrkesskade
    }


    private fun beregn(
        inntekterPerÅr: Set<InntektPerÅr>
    ): Grunnlag11_19 {
        val grunnlag11_19 =
            GrunnlagetForBeregningen(inntekterPerÅr).beregnGrunnlaget()

        return grunnlag11_19
    }
}
