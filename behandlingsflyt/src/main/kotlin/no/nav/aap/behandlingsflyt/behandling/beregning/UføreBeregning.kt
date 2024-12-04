package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.UføreInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.Year

class UføreBeregning(
    private val grunnlag: Grunnlag11_19,
    private val uføregrad: Prosent,
    private val inntekterForegåendeÅr: Set<InntektPerÅr>,
) {

    init {
        require(uføregrad < Prosent.`100_PROSENT`) { "Uføregraden må være mindre enn 100 prosent" }
    }

    fun beregnUføre(ytterligereNedsattÅr: Year): GrunnlagUføre {

        val oppjusterteInntekter = oppjusterMhpUføregrad(inntekterForegåendeÅr)

        // 6G-begrensning ligger her samt gjennomsnitt
        val ytterligereNedsattGrunnlag = beregn11_19Grunnlag(oppjusterteInntekter)

        if (grunnlag.grunnlaget() >= ytterligereNedsattGrunnlag.grunnlaget()) {
            return GrunnlagUføre(
                grunnlaget = grunnlag.grunnlaget(),
                type = GrunnlagUføre.Type.STANDARD,
                grunnlag = grunnlag,
                grunnlagYtterligereNedsatt = ytterligereNedsattGrunnlag,
                uføregrad = uføregrad,
                uføreInntekterFraForegåendeÅr = oppjusterteInntekter,
                uføreYtterligereNedsattArbeidsevneÅr = ytterligereNedsattÅr
            )
        } else {
            return GrunnlagUføre(
                grunnlaget = ytterligereNedsattGrunnlag.grunnlaget(),
                type = GrunnlagUføre.Type.YTTERLIGERE_NEDSATT,
                grunnlag = grunnlag,
                grunnlagYtterligereNedsatt = ytterligereNedsattGrunnlag,
                uføregrad = uføregrad,
                uføreInntekterFraForegåendeÅr = oppjusterteInntekter,
                uføreYtterligereNedsattArbeidsevneÅr = ytterligereNedsattÅr
            )
        }
    }

    private fun oppjusterMhpUføregrad(ikkeOppjusterteInntekter: Set<InntektPerÅr>): List<UføreInntekt> {
        return ikkeOppjusterteInntekter.map { inntekt ->
            val arbeidsgrad = uføregrad.komplement()
            UføreInntekt(
                år = inntekt.år,
                inntektIKroner = inntekt.beløp,
                uføregrad = uføregrad,
                arbeidsgrad = arbeidsgrad,
                inntektJustertForUføregrad = inntekt.beløp.dividert(arbeidsgrad),
                inntektIG = inntekt.gUnit().gUnit,
                grunnbeløp = inntekt.gUnit().beløp,
                inntektIGJustertForUføregrad = inntekt.gUnit().gUnit.dividert(arbeidsgrad)
            )
        }
    }

    private fun beregn11_19Grunnlag(
        oppjusterteInntekter: List<UføreInntekt>
    ): Grunnlag11_19 {
        val oppjusterteInntekterPerÅr = oppjusterteInntekter.map { inntekt ->
            InntektPerÅr(
                år = inntekt.år,
                beløp = inntekt.inntektJustertForUføregrad
            )
        }.toSet()
        return GrunnlagetForBeregningen(oppjusterteInntekterPerÅr).beregnGrunnlaget()
    }
}
