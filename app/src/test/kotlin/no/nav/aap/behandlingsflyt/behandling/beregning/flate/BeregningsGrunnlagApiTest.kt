package no.nav.aap.behandlingsflyt.behandling.beregning.flate

import no.nav.aap.behandlingsflyt.behandling.beregning.Beregning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

import java.time.LocalDate

class BeregningsGrunnlagApiTest {

    @Test
    fun beregningsGrunnlagApi() {
        val input = Inntektsbehov(
            Input(
                nedsettelsesDato = LocalDate.of(2023, 1, 1),
                inntekter = setOf(
                    InntektPerÅr(2022, Beløp(500000)),
                    InntektPerÅr(2021, Beløp(400000)),
                    InntektPerÅr(2020, Beløp(300000))
                ),
                uføregrad = Prosent(30),
                yrkesskadevurdering = Yrkesskadevurdering(
                    begrunnelse = "en begrunnelse",
                    andelAvNedsettelse = Prosent(30),
                    erÅrsakssammenheng = true,
                    skadetidspunkt = LocalDate.of(2021, 1, 1),
                ),
                beregningVurdering = BeregningVurdering(
                    begrunnelse = "test",
                    ytterligereNedsattArbeidsevneDato = LocalDate.of(2023, 1, 1),
                    antattÅrligInntekt = Beløp(500000)
                )
            )
        )

        val beregning = Beregning(input).beregneMedInput()

        val res = beregningDTO(beregning)

        val grunnlag1119 = requireNotNull(res.grunnlagYrkesskadeUføre)
        assertThat(grunnlag1119.beregningsgrunnlag.grunnlag.inntekter).hasSize(3)
        assertThat(grunnlag1119.beregningsgrunnlag.grunnlag.inntekter["2022"]).isEqualByComparingTo(BigDecimal(500000))
        assertThat(grunnlag1119.beregningsgrunnlag.grunnlag.inntekter["2021"]).isEqualByComparingTo(BigDecimal(400000))
        assertThat(grunnlag1119.beregningsgrunnlag.grunnlag.inntekter["2020"]).isEqualByComparingTo(BigDecimal(300000))
    }
}