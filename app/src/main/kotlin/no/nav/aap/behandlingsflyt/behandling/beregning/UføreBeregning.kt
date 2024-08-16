package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.Prosent
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
        val ytterligereNedsattGrunnlag = beregn11_19Grunnlag(oppjusterteInntekter.toSet())

        //TODO: Gang med årets G
        // Hvilket år? Søknads-dato? Uføredato? Ytterligere nedsatt år?
        val uføreInntektIKroner = grunnlag.grunnlaget().multiplisert(Beløp(10))

        if (grunnlag.grunnlaget() >= ytterligereNedsattGrunnlag.grunnlaget()) {
            return GrunnlagUføre(
                grunnlaget = grunnlag.grunnlaget(),
                type = GrunnlagUføre.Type.STANDARD,
                grunnlag = grunnlag,
                grunnlagYtterligereNedsatt = ytterligereNedsattGrunnlag,
                uføregrad = uføregrad,
                uføreInntekterFraForegåendeÅr = inntekterForegåendeÅr.toList(), //TODO: wat?
                uføreInntektIKroner = uføreInntektIKroner,
                uføreYtterligereNedsattArbeidsevneÅr = ytterligereNedsattÅr,
                erGjennomsnitt = grunnlag.erGjennomsnitt()
            )
        } else {
            return GrunnlagUføre(
                grunnlaget = ytterligereNedsattGrunnlag.grunnlaget(),
                type = GrunnlagUføre.Type.YTTERLIGERE_NEDSATT,
                grunnlag = grunnlag,
                grunnlagYtterligereNedsatt = ytterligereNedsattGrunnlag,
                uføregrad = uføregrad,
                uføreInntekterFraForegåendeÅr = inntekterForegåendeÅr.toList(), //TODO: wat? <- hva menes med wat?
                uføreInntektIKroner = uføreInntektIKroner,
                uføreYtterligereNedsattArbeidsevneÅr = ytterligereNedsattÅr,
                erGjennomsnitt = grunnlag.erGjennomsnitt()
            )
        }
    }

    private fun oppjusterMhpUføregrad(ikkeOppjusterteInntekter: Set<InntektPerÅr>) =
        ikkeOppjusterteInntekter.map {
            InntektPerÅr(it.år, it.beløp.dividert(uføregrad.komplement()))
        }


    private fun beregn11_19Grunnlag(
        inntekterPerÅr: Set<InntektPerÅr>
    ): Grunnlag11_19 {
        val grunnlag11_19 =
            GrunnlagetForBeregningen(inntekterPerÅr).beregnGrunnlaget()

        return grunnlag11_19
    }
}
