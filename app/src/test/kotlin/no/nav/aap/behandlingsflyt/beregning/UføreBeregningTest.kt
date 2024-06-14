package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Year

class UføreBeregningTest {

    @Test
    fun `Hvis uføregraden er 0 prosent, endres ikke grunnlaget`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = Grunnlag11_19(GUnit(4),false,false),
            ytterligereNedsattGrunnlag = Grunnlag11_19(
                GUnit(5),false,false
            ),
            uføregrad = Prosent.`0_PROSENT`,
            inntekterForegåendeÅr = setOf(
                InntektPerÅr(Year.of(2021), Beløp(5))
            )
        )

        val grunnlagUføre = uføreBeregning.beregnUføre(Year.of(2021))

        assertThat(grunnlagUføre.underliggendeYtterligereNedsatt().grunnlaget()).isEqualTo(GUnit(5))
    }

    @Test
    fun `Hvis bruker har en uføregrad på 100 prosent, skal ikke uføreberegningen gjøres`() {
        assertThrows<IllegalArgumentException> {
            UføreBeregning(
                grunnlag = Grunnlag11_19(GUnit(4),false,false),
                ytterligereNedsattGrunnlag = Grunnlag11_19(
                    GUnit(4),false,false
                ),
                uføregrad = Prosent.`100_PROSENT`,
                inntekterForegåendeÅr = setOf(
                    InntektPerÅr(Year.of(2021), Beløp(5))
                )
            )
        }
    }

    @Test
    fun `Hvis bruker hadde høyere inntekt ved ytterligere nedsatt, justert for uføregrad, brukes inntekter fra ytteligere nedsatt`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = Grunnlag11_19(GUnit(4),false,false),
            ytterligereNedsattGrunnlag = Grunnlag11_19(
                GUnit("3.5"),false,false
            ),
            uføregrad = Prosent.`30_PROSENT`,
            inntekterForegåendeÅr = setOf(
                InntektPerÅr(Year.now().minusYears(1), Beløp(5))
            )
        )

        val grunnlagUføre = uføreBeregning.beregnUføre(
            Year.now()
        )

        assertThat(grunnlagUføre.grunnlaget()).isEqualTo(GUnit(4)) //TODO: er denne testen riktig?
    }

    @Test
    fun `Hvis bruker hadde lavere inntekt ved ytterligere nedsatt, justert for uføregrad, brukes inntekter fra nedsatt med halvparten`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = Grunnlag11_19(GUnit(4),
                false,
                false),
            ytterligereNedsattGrunnlag = Grunnlag11_19(
                GUnit("5"),false,false
            ),
            uføregrad = Prosent.`30_PROSENT`,
            inntekterForegåendeÅr = setOf(
                InntektPerÅr(Year.now().minusYears(1), Beløp(5))
            )
        )

        val grunnlagUføre = uføreBeregning.beregnUføre(Year.now())

        assertThat(grunnlagUføre.underliggendeYtterligereNedsatt().grunnlaget()).isEqualTo(GUnit(5)) //TODO: er denne testen riktig
    }
}
