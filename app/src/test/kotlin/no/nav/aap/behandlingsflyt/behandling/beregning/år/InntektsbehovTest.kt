package no.nav.aap.behandlingsflyt.behandling.beregning.år

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
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
                beregningGrunnlag = null,
                registrerteYrkesskader = null
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
                null,
                BeregningGrunnlag(
                    tidspunktVurdering = BeregningstidspunktVurdering(
                        begrunnelse = "asdf",
                        ytterligereNedsattArbeidsevneDato = ytterligereNedsattDato,
                        nedsattArbeidsevneDato = nedsettelsesDato,
                        ytterligereNedsattBegrunnelse = "asdf"
                    ), yrkesskadeBeløpVurdering = null
                ),
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
                registrerteYrkesskader = null,
                beregningGrunnlag = BeregningGrunnlag(
                    tidspunktVurdering = BeregningstidspunktVurdering(
                        begrunnelse = "begrunnelse",
                        nedsattArbeidsevneDato = nedsettelsesDato,
                        ytterligereNedsattArbeidsevneDato = LocalDate.now().minusYears(10),
                        ytterligereNedsattBegrunnelse = "begrunnelse"
                    ), yrkesskadeBeløpVurdering = null
                )
            )
        )

        assertThat(inntektsbehov.finnesUføreData()).isTrue()
    }
}