package no.nav.aap.behandlingsflyt.behandling.beregning.år

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurdering
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Year

class InntektsbehovTest {

    @Test
    fun `skal utlede de tre forutgående kalenderårene fra nedsettelsesdato`() {
        val nedsettelsesDato = LocalDate.now().minusYears(3)
        val relevanteÅr = Inntektsbehov(
            Input(
                nedsettelsesDato,
                inntekter = setOf(),
                uføregrad = Prosent.`0_PROSENT`,
                yrkesskadevurdering = null,
                beregningVurdering = null
            )
        ).utledAlleRelevanteÅr()

        val nedsattYear = Year.of(nedsettelsesDato.year)

        assertThat(relevanteÅr).hasSize(3)
        assertThat(relevanteÅr).containsExactly(
            nedsattYear.minusYears(3),
            nedsattYear.minusYears(2),
            nedsattYear.minusYears(1)
        )
    }

    @Test
    fun `skal utlede de tre forutgående kalenderårene fra nedsettelsesdato og tre forutgående kalenderårene fra ytterligere nedsattdato`() {
        val nedsettelsesDato = LocalDate.now().minusYears(6)
        val ytterligereNedsattDato = LocalDate.now().minusYears(2)
        val relevanteÅr = Inntektsbehov(
            Input(
                nedsettelsesDato,
                setOf(),
                Prosent.`0_PROSENT`,
                null,
                BeregningVurdering(
                    begrunnelse = "asdf",
                    ytterligereNedsattArbeidsevneDato = ytterligereNedsattDato,
                    antattÅrligInntekt = Beløp(100)
                )
            )
        ).utledAlleRelevanteÅr()

        val nedsattYear = Year.of(nedsettelsesDato.year)
        val ytterligereNedsattYear = Year.of(ytterligereNedsattDato.year)

        assertThat(relevanteÅr).hasSize(6)
        assertThat(relevanteÅr).containsExactly(
            nedsattYear.minusYears(3),
            nedsattYear.minusYears(2),
            nedsattYear.minusYears(1),
            ytterligereNedsattYear.minusYears(3),
            ytterligereNedsattYear.minusYears(2),
            ytterligereNedsattYear.minusYears(1)
        )
    }

    @Test
    fun `om det finnes uføredata, skal det oppgis`() {
        val nedsettelsesDato = LocalDate.now().minusYears(3)
        val inntektsbehov = Inntektsbehov(
            Input(
                nedsettelsesDato,
                inntekter = setOf(),
                uføregrad = Prosent.`30_PROSENT`,
                yrkesskadevurdering = null,
                beregningVurdering = BeregningVurdering(
                    begrunnelse = "begrunnelse",
                    antattÅrligInntekt = Beløp(123456),
                    ytterligereNedsattArbeidsevneDato = LocalDate.now().minusYears(10)
                )
            )
        )

        assertThat(inntektsbehov.finnesUføreData()).isTrue()
    }
}