package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDate
import java.time.Year

class Beregning(
    private val input: Inntektsbehov
) {
    internal fun beregneMedInput(): Beregningsgrunnlag {
        val grunnlag11_19 = beregn(input.utledForOrdinær())//6G begrensning ligger her samt gjennomsnitt

        val beregningMedEllerUtenUføre = if (input.skalBeregneMedUføre()) {
            val ikkeOppjusterteInntekter = input.utledForYtterligereNedsatt()
            val oppjusterteInntekter = ikkeOppjusterteInntekter.map {
                InntektPerÅr(it.år, it.beløp.dividert(input.uføregrad().kompliment()))
            }
            val beregningVedUføre = beregn(oppjusterteInntekter.toSet())// år kommer herfra //6G begrensning ligger her samt gjennomsnitt
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
