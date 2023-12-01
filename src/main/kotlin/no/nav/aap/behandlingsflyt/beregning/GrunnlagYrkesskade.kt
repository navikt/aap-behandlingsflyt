package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.GUnit

class GrunnlagYrkesskade(
    private val grunnlaget: GUnit,
    private val grunnlag11_19: Grunnlag11_19
) : Beregningsgrunnlag {

    override fun grunnlaget(): GUnit {
        return grunnlaget
    }
}
