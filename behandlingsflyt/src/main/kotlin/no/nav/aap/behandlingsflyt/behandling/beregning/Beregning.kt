package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import java.time.Year

class Beregning(
    private val input: Inntektsbehov
) {
    fun beregneMedInput(): Beregningsgrunnlag {
        //6G begrensning ligger her samt gjennomsnitt
        val grunnlag11_19 = beregn11_19Grunnlag(input.utledForOrdinær())

        val beregningMedEllerUtenUføre = if (input.finnesUføreData()) {
            val inntekterFørYtterligereNedsattDato = input.utledForYtterligereNedsatt()

            val uføreberegning = UføreBeregning(
                grunnlag = grunnlag11_19,
                uføregrader = input.uføregrad(),
                inntekterForegåendeÅr = inntekterFørYtterligereNedsattDato,
                inntektsPerioder = input.inntektsPerioder(),
            )
            val ytterligereNedsattArbeidsevneDato = input.hentYtterligereNedsattArbeidsevneDato()
            requireNotNull(ytterligereNedsattArbeidsevneDato)

            val grunnlagUføre = uføreberegning.beregnUføre(Year.from(ytterligereNedsattArbeidsevneDato))
            grunnlagUføre
        } else {
            grunnlag11_19
        }

        // §11-22 Arbeidsavklaringspenger ved yrkesskade
        val beregningMedEllerUtenUføreMedEllerUtenYrkesskade =
            if (input.yrkesskadeVurderingEksisterer()) {
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

    private fun beregn11_19Grunnlag(
        inntekterPerÅr: Set<InntektPerÅr>
    ): Grunnlag11_19 {
        val grunnlag11_19 =
            GrunnlagetForBeregningen(inntekterPerÅr).beregnGrunnlaget()

        return grunnlag11_19
    }
}
