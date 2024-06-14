package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Prosent
import java.time.Year

class UføreBeregning(
    private val grunnlag: Grunnlag11_19,
    private val ytterligereNedsattGrunnlag: Grunnlag11_19,
    private val uføregrad: Prosent,
    private val inntekterForegåendeÅr: Set<InntektPerÅr>,
) {

    init {
        require(uføregrad < Prosent.`100_PROSENT`) { "Uføregraden må være mindre enn 100 prosent" }
    }

    fun beregnUføre(ytterligereNedsattÅr: Year): GrunnlagUføre {
        if (grunnlag.grunnlaget() >= ytterligereNedsattGrunnlag.grunnlaget()) {
            return GrunnlagUføre(
                grunnlaget = grunnlag.grunnlaget(),
                type = GrunnlagUføre.Type.STANDARD,
                grunnlag = grunnlag,
                grunnlagYtterligereNedsatt = ytterligereNedsattGrunnlag,
                uføregrad= uføregrad,
                uføreInntekterFraForegåendeÅr = inntekterForegåendeÅr.toList(), //TODO: wat?
                uføreInntektIKroner = grunnlag.grunnlaget().multiplisert(Beløp(10)), //TODO: Gang med årets g
                uføreYtterligereNedsattArbeidsevneÅr = ytterligereNedsattÅr,
                er6GBegrenset = grunnlag.er6GBegrenset(),
                erGjennomsnitt = grunnlag.erGjennomsnitt()
            )

        } else {
            return GrunnlagUføre(
                grunnlaget = grunnlag.grunnlaget(),
                type = GrunnlagUføre.Type.YTTERLIGERE_NEDSATT,
                grunnlag = grunnlag,
                grunnlagYtterligereNedsatt = ytterligereNedsattGrunnlag,
                uføregrad = uføregrad,
                uføreInntekterFraForegåendeÅr = inntekterForegåendeÅr.toList(), //TODO: wat?
                uføreInntektIKroner = grunnlag.grunnlaget().multiplisert(Beløp(10)), //TODO: Gang med årets g
                uføreYtterligereNedsattArbeidsevneÅr = ytterligereNedsattÅr,er6GBegrenset = grunnlag.er6GBegrenset(),
                erGjennomsnitt = grunnlag.erGjennomsnitt()
            )

        }
    }
}
