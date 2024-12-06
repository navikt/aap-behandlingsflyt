package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningYrkeskaderBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
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
                    andelAvNedsettelsen = Prosent(30),
                    erÅrsakssammenheng = true,
                    relevanteSaker = listOf("yrkesskadesaken")
                ),
                beregningGrunnlag = BeregningGrunnlag(
                    tidspunktVurdering = BeregningstidspunktVurdering(
                        begrunnelse = "test",
                        ytterligereNedsattArbeidsevneDato = LocalDate.of(2023, 1, 1),
                        ytterligereNedsattBegrunnelse = "test",
                        nedsattArbeidsevneDato = LocalDate.of(2023, 1, 1),
                    ),
                    yrkesskadeBeløpVurdering = BeregningYrkeskaderBeløpVurdering(
                        vurderinger = listOf(
                            YrkesskadeBeløpVurdering(
                                antattÅrligInntekt = Beløp(50000),
                                referanse = "yrkesskadesaken",
                                begrunnelse = "asdf"
                            )
                        )
                    )
                ),
                registrerteYrkesskader = Yrkesskader(
                    listOf(
                        Yrkesskade(
                            ref = "yrkesskadesaken",
                            skadedato = LocalDate.of(2021, 1, 1)
                        )
                    )
                )

            )
        )

        val beregning = Beregning(input).beregneMedInput()

        val res = beregningDTO(beregning)

        val grunnlagYrkesskadeUføre = requireNotNull(res.grunnlagYrkesskadeUføre)
        fun inntektForÅr(år: String): BigDecimal {
            return grunnlagYrkesskadeUføre.uføreGrunnlag.inntekter.single { it.år == år }.inntektIKroner
        }
        assertThat(grunnlagYrkesskadeUføre.uføreGrunnlag.inntekter).hasSize(3)
        assertThat(inntektForÅr("2022")).isEqualByComparingTo(BigDecimal(500000))
        assertThat(inntektForÅr("2021")).isEqualByComparingTo(BigDecimal(400000))
        assertThat(inntektForÅr("2020")).isEqualByComparingTo(BigDecimal(300000))
    }
}