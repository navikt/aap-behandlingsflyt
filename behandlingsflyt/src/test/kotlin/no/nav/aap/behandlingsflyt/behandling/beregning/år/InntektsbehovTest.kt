package no.nav.aap.behandlingsflyt.behandling.beregning.år

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Year

class InntektsbehovTest {
    @Test
    fun `henter ut relevante år, nemlig tre år før nedsettelsen`() {
        val nedsettelsesDato = LocalDate.now().withYear(2005)
        val forOrdinær = Inntektsbehov(
            Input(
                nedsettelsesDato,
                inntekter = setOf(
                    InntektPerÅr(nedsettelsesDato.plusYears(1).year, Beløp(123)),
                    InntektPerÅr(nedsettelsesDato.minusYears(0).year, Beløp(125)),
                    InntektPerÅr(nedsettelsesDato.minusYears(1).year, Beløp(126)),
                    InntektPerÅr(nedsettelsesDato.minusYears(2).year, Beløp(127)),
                    InntektPerÅr(nedsettelsesDato.minusYears(3).year, Beløp(128)),
                    InntektPerÅr(nedsettelsesDato.minusYears(4).year, Beløp(129))
                ),
                uføregrad = listOf(Uføre(LocalDate.now(), Prosent.`0_PROSENT`)),
                yrkesskadevurdering = null,
                beregningGrunnlag = null,
                registrerteYrkesskader = null
            )
        ).utledForOrdinær()

        assertThat(forOrdinær).containsExactlyInAnyOrder(
            InntektPerÅr(2004, Beløp(126)),
            InntektPerÅr(2003, Beløp(127)),
            InntektPerÅr(2002, Beløp(128))
        )
    }

    @Test
    fun `skal utlede de tre forutgående kalenderårene fra nedsettelsesdato`() {
        val nedsettelsesDato = LocalDate.now().minusYears(3)
        val relevanteÅr = Inntektsbehov(
            Input(
                nedsettelsesDato,
                inntekter = emptySet(),
                uføregrad = listOf(Uføre(LocalDate.now(), Prosent.`0_PROSENT`)),
                yrkesskadevurdering = null,
                beregningGrunnlag = null,
                registrerteYrkesskader = null
            )
        ).utledAlleRelevanteÅr()

        val nedsattYear = Year.of(nedsettelsesDato.year)

        assertThat(relevanteÅr).hasSize(3)
        assertThat(relevanteÅr).containsExactlyInAnyOrder(
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
                emptySet(),
                listOf(Uføre(LocalDate.now(), Prosent.`0_PROSENT`)),
                null,
                null,
                BeregningGrunnlag(
                    tidspunktVurdering = BeregningstidspunktVurdering(
                        begrunnelse = "asdf",
                        ytterligereNedsattArbeidsevneDato = ytterligereNedsattDato,
                        nedsattArbeidsevneDato = nedsettelsesDato,
                        ytterligereNedsattBegrunnelse = "asdf",
                        vurdertAv = "saksbehandler"
                    ), yrkesskadeBeløpVurdering = null
                ),
            )
        ).utledAlleRelevanteÅr()

        val nedsattYear = Year.of(nedsettelsesDato.year)
        val ytterligereNedsattYear = Year.of(ytterligereNedsattDato.year)

        assertThat(relevanteÅr).hasSize(6)
        assertThat(relevanteÅr).containsExactlyInAnyOrder(
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
                inntekter = emptySet(),
                uføregrad = listOf(Uføre(LocalDate.now(), Prosent.`30_PROSENT`)),
                yrkesskadevurdering = null,
                registrerteYrkesskader = null,
                beregningGrunnlag = BeregningGrunnlag(
                    tidspunktVurdering = BeregningstidspunktVurdering(
                        begrunnelse = "begrunnelse",
                        nedsattArbeidsevneDato = nedsettelsesDato,
                        ytterligereNedsattArbeidsevneDato = LocalDate.now().minusYears(10),
                        ytterligereNedsattBegrunnelse = "begrunnelse",
                        vurdertAv = "saksbehandler"
                    ), yrkesskadeBeløpVurdering = null
                )
            )
        )

        assertThat(inntektsbehov.finnesUføreData()).isTrue()
    }
}
