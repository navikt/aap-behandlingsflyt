package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.GUnit
import no.nav.aap.behandlingsflyt.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UføreBeregningTest {

    @Test
    fun `Hvis uføregraden er 0 prosent, endres ikke grunnlaget`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = Grunnlag11_19(GUnit(4)),
            ytterligereNedsattGrunnlag = Grunnlag11_19(GUnit(5)),
            uføregrad = Prosent.`0_PROSENT`
        )

        val grunnlagUføre = uføreBeregning.beregnUføre()

        assertThat(grunnlagUføre.grunnlaget()).isEqualTo(GUnit(5))
    }

    @Test
    fun `Hvis bruker har en uføregrad på 100 prosent, skal ikke uføreberegningen gjøres`() {
        assertThrows<IllegalArgumentException> {
            UføreBeregning(
                grunnlag = Grunnlag11_19(GUnit(4)),
                ytterligereNedsattGrunnlag = Grunnlag11_19(GUnit(4)),
                uføregrad = Prosent.`100_PROSENT`
            )
        }
    }

    @Test
    fun `Hvis bruker hadde høyere inntekt ved ytterligere nedsatt, justert for uføregrad, brukes inntekter fra ytteligere nedsatt`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = Grunnlag11_19(GUnit(4)),
            ytterligereNedsattGrunnlag = Grunnlag11_19(GUnit("3.5")),
            uføregrad = Prosent.`30_PROSENT`
        )

        val grunnlagUføre = uføreBeregning.beregnUføre()

        assertThat(grunnlagUføre.grunnlaget()).isEqualTo(GUnit(5))
    }

    @Test
    fun `Hvis bruker hadde lavere inntekt ved ytterligere nedsatt, justert for uføregrad, brukes inntekter fra nedsatt med halvparten`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = Grunnlag11_19(GUnit(4)),
            ytterligereNedsattGrunnlag = Grunnlag11_19(GUnit("3.5")),
            uføregrad = Prosent.`30_PROSENT`
        )

        val grunnlagUføre = uføreBeregning.beregnUføre()

        assertThat(grunnlagUføre.grunnlaget()).isEqualTo(GUnit(5))
    }
}
