package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Year

class UføreBeregningTest {

    @Test
    fun `Hvis bruker har en uføregrad på 100 prosent, skal ikke uføreberegningen gjøres`() {
        assertThrows<IllegalArgumentException> {
            UføreBeregning(
                grunnlag = Grunnlag11_19(
                    grunnlaget = GUnit(4),
                    erGjennomsnitt = false,
                    gjennomsnittligInntektIG = GUnit(0),
                    inntekter = emptyList()
                ),
                uføregrad = Prosent.`100_PROSENT`,
                inntekterForegåendeÅr = setOf(
                    InntektPerÅr(
                        Year.of(2022),
                        Beløp(BigDecimal(5 * 109_784)) // 548 920
                    ),
                    InntektPerÅr(
                        Year.of(2021),
                        Beløp(BigDecimal(5 * 104_716)) // 209 432
                    ),
                    InntektPerÅr(
                        Year.of(2020),
                        Beløp(BigDecimal(5 * 100_853)) // 201 706
                    )
                )
            )
        }
    }

    @Test
    fun `Hvis bruker hadde høyere inntekt ved ytterligere nedsatt, justert for uføregrad, brukes inntekter fra ytteligere nedsatt`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = Grunnlag11_19(
                grunnlaget = GUnit(4),
                erGjennomsnitt = false,
                gjennomsnittligInntektIG = GUnit(0),
                inntekter = emptyList()
            ),
            uføregrad = Prosent.`30_PROSENT`,
            inntekterForegåendeÅr = setOf(
                InntektPerÅr(
                    Year.of(2022),
                    Beløp(BigDecimal(5 * 109_784).multiply(BigDecimal("0.7"))) // 548 920
                ),
                InntektPerÅr(
                    Year.of(2021),
                    Beløp(BigDecimal(5 * 104_716).multiply(BigDecimal("0.7"))) // 209 432
                ),
                InntektPerÅr(
                    Year.of(2020),
                    Beløp(BigDecimal(5 * 100_853).multiply(BigDecimal("0.7"))) // 201 706
                )
            )
        )

        val grunnlagUføre = uføreBeregning.beregnUføre(Year.of(2023))

        assertThat(grunnlagUføre.grunnlaget()).isEqualTo(GUnit(5))
        assertThat(grunnlagUføre.type()).isEqualTo(GrunnlagUføre.Type.YTTERLIGERE_NEDSATT)
    }

    @Test
    fun `Hvis bruker hadde lavere inntekt ved ytterligere nedsatt, justert for uføregrad, brukes inntekter fra nedsatt med halvparten`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = Grunnlag11_19(
                grunnlaget = GUnit(5),
                erGjennomsnitt = false,
                gjennomsnittligInntektIG = GUnit(0),
                inntekter = emptyList()
            ),
            uføregrad = Prosent.`30_PROSENT`,
            inntekterForegåendeÅr = setOf(
                InntektPerÅr(
                    Year.of(2022),
                    Beløp(BigDecimal(4 * 109_784).multiply(BigDecimal("0.7"))) // 448 920
                ),
                InntektPerÅr(
                    Year.of(2021),
                    Beløp(BigDecimal(4 * 104_716).multiply(BigDecimal("0.7"))) // 209 432
                ),
                InntektPerÅr(
                    Year.of(2020),
                    Beløp(BigDecimal(4 * 100_853).multiply(BigDecimal("0.7"))) // 201 706
                )
            )
        )

        val grunnlagUføre = uføreBeregning.beregnUføre(Year.of(2023))

        assertThat(grunnlagUføre.grunnlaget()).isEqualTo(GUnit(5))
        assertThat(grunnlagUføre.type()).isEqualTo(GrunnlagUføre.Type.STANDARD)
    }
}
