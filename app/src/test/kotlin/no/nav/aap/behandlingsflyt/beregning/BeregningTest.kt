package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurdering
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregningTest {

    @Test
    fun `beregn input med basic 11_19 uten ys eller uføre`() {
        val input = Inntektsbehov(
            input = Input(
                nedsettelsesDato = LocalDate.of(2023, 1, 1),
                inntekter = setOf(
                    InntektPerÅr(2022, Beløp(500000)),
                    InntektPerÅr(2021, Beløp(400000)),
                    InntektPerÅr(2020, Beløp(300000))
                ),
                uføregrad = null,
                yrkesskadevurdering = null,
                beregningVurdering = null
            )
        )

        val beregning = Beregning(input).beregneMedInput()

        assertThat(beregning.grunnlaget()).isEqualTo(GUnit("4.5543977264"))
    }

    @Test
    fun `oppjusterer grunnlaget ved uføre`() {
        val input = Inntektsbehov(
            Input(
                nedsettelsesDato = LocalDate.of(2015, 1, 1),
                inntekter = setOf(
                    InntektPerÅr(2022, Beløp(500000)),
                    InntektPerÅr(2021, Beløp(400000)),
                    InntektPerÅr(2020, Beløp(300000))
                ),
                uføregrad = Prosent(30),
                yrkesskadevurdering = null,
                beregningVurdering = BeregningVurdering(
                    begrunnelse = "test",
                    ytterligereNedsattArbeidsevneDato = LocalDate.of(2023, 1, 1),
                    antattÅrligInntekt = Beløp(500000)
                )
            )
        )

        val beregning = Beregning(input).beregneMedInput()

        assertThat(beregning.grunnlaget()).isEqualTo(GUnit("6"))

    }
}