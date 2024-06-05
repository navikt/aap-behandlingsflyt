package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.verdityper.Prosent
import java.time.Year

class UføreBeregning(
    private val grunnlag: Beregningsgrunnlag,
    private val ytterligereNedsattGrunnlag: Beregningsgrunnlag,
    private val uføregrad: Prosent
) {

    init {
        require(uføregrad < Prosent.`100_PROSENT`) { "Uføregraden må være mindre enn 100 prosent" }
    }

    fun beregnUføre(): GrunnlagUføre {
        val oppjustertGrunnlagVedUføre = ytterligereNedsattGrunnlag.grunnlaget().dividert(uføregrad.kompliment()) //uføreOppjusterteInntekter men feil

        if (grunnlag.grunnlaget() >= oppjustertGrunnlagVedUføre) {
            return GrunnlagUføre(
                grunnlaget = grunnlag.grunnlaget(),
                gjeldende = GrunnlagUføre.Type.STANDARD,
                grunnlag = grunnlag,
                grunnlagYtterligereNedsatt = ytterligereNedsattGrunnlag
            )

        } else {
            return GrunnlagUføre(
                grunnlaget = oppjustertGrunnlagVedUføre,
                gjeldende = GrunnlagUføre.Type.YTTERLIGERE_NEDSATT,
                grunnlag = grunnlag,
                grunnlagYtterligereNedsatt = ytterligereNedsattGrunnlag
            )

        }
    }
}
