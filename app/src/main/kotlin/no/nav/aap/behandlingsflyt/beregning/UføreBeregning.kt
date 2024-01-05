package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.verdityper.Prosent

class UføreBeregning(
    private val grunnlag: Beregningsgrunnlag,
    private val ytterligereNedsattGrunnlag: Beregningsgrunnlag,
    private val uføregrad: Prosent
) {

    init {
        require(uføregrad < Prosent.`100_PROSENT`) { "Uføregraden må være mindre enn 100 prosent" }
    }

    fun beregnUføre(): GrunnlagUføre {
        val oppjustertGrunnlagVedUføre = ytterligereNedsattGrunnlag.grunnlaget().dividert(uføregrad.kompliment())

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
