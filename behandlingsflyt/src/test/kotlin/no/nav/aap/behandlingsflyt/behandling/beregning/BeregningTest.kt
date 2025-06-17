package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Input
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningYrkeskaderBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Year

class BeregningTest {

    @Test
    fun `beregn input med basic 11_19 uten yrkesskade eller uføre`() {
        val input = Inntektsbehov(
            input = Input(
                nedsettelsesDato = LocalDate.of(2023, 1, 1),
                inntekter = setOf(
                    InntektPerÅr(2022, Beløp(500000)),
                    InntektPerÅr(2021, Beløp(400000)),
                    InntektPerÅr(2020, Beløp(300000))
                ),
                uføregrad = emptyList(),
                yrkesskadevurdering = null,
                beregningGrunnlag = null,
                registrerteYrkesskader = null
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
                uføregrad = listOf(Uføre(LocalDate.now(), Prosent(30))),
                yrkesskadevurdering = null,
                beregningGrunnlag = BeregningGrunnlag(
                    tidspunktVurdering = BeregningstidspunktVurdering(
                        begrunnelse = "test",
                        nedsattArbeidsevneDato = LocalDate.of(2023, 1, 1),
                        ytterligereNedsattBegrunnelse = "test2",
                        ytterligereNedsattArbeidsevneDato = LocalDate.of(2023, 1, 1),
                        vurdertAv = "saksbehandler"
                    ), yrkesskadeBeløpVurdering = null
                ),
                registrerteYrkesskader = null
            )
        )

        val beregning = Beregning(input).beregneMedInput()

        assertThat(beregning.grunnlaget()).isEqualTo(GUnit("6"))
    }

    @Test
    fun `beregning med kun yrkesskade, ikke uføre`() {
        val inntekterPerÅr = setOf(
            InntektPerÅr(Year.of(2022), Beløp(3 * 109_784)),
            InntektPerÅr(Year.of(2021), Beløp(4 * 104_716)),
            InntektPerÅr(Year.of(2020), Beløp(5 * 100_853))
        )
        val input = Inntektsbehov(
            Input(
                nedsettelsesDato = LocalDate.of(2023, 1, 1),
                inntekter = inntekterPerÅr,
                uføregrad = emptyList(),
                yrkesskadevurdering = Yrkesskadevurdering(
                    begrunnelse = "en begrunnelse",
                    andelAvNedsettelsen = Prosent(40),
                    erÅrsakssammenheng = true,
                    relevanteSaker = listOf("yrkesskadesaken"),
                    vurdertAv = "saksbehandler"
                ),
                beregningGrunnlag = BeregningGrunnlag(
                    tidspunktVurdering = BeregningstidspunktVurdering(
                        begrunnelse = "test",
                        nedsattArbeidsevneDato = LocalDate.of(2023, 1, 1),
                        ytterligereNedsattArbeidsevneDato = null,
                        ytterligereNedsattBegrunnelse = null,
                        vurdertAv = "saksbehandler"
                    ),
                    yrkesskadeBeløpVurdering = BeregningYrkeskaderBeløpVurdering(
                        vurderinger = listOf(
                            YrkesskadeBeløpVurdering(
                                antattÅrligInntekt = Beløp(500000),
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
                            skadedato = LocalDate.of(2019, 1, 1)
                        )
                    )
                )
            )
        )

        val beregning = Beregning(input).beregneMedInput()
        // Riktig svar bør være: 0.4*(500000/98866) + 0.6*4 = 4,4229401412
        // Forklaring: første ledd kommer fra yrkesskade (justert til GUnit) pluss gjennomsnitt av inntekt
        // siste 3 år ganget med 0.6 (arbeidsgrad).
        assertThat(beregning.grunnlaget()).isEqualTo(GUnit("4.4229401412"))
    }

    @Test
    fun `Beregning med både uføre og yrkesskade`() {
        val input = Inntektsbehov(
            Input(
                nedsettelsesDato = LocalDate.of(2023, 1, 1),
                inntekter = setOf(
                    InntektPerÅr(2022, Beløp(500000)),
                    InntektPerÅr(2021, Beløp(400000)),
                    InntektPerÅr(2020, Beløp(300000))
                ),
                uføregrad = listOf(Uføre(LocalDate.now(), Prosent(50))),
                yrkesskadevurdering = Yrkesskadevurdering(
                    begrunnelse = "en begrunnelse",
                    andelAvNedsettelsen = Prosent(30),
                    erÅrsakssammenheng = true,
                    relevanteSaker = listOf("yrkesskadesaken"),
                    vurdertAv = "saksbehandler"
                ),
                beregningGrunnlag = BeregningGrunnlag(
                    tidspunktVurdering = BeregningstidspunktVurdering(
                        begrunnelse = "test",
                        nedsattArbeidsevneDato = LocalDate.of(2023, 1, 1),
                        ytterligereNedsattBegrunnelse = "test2",
                        ytterligereNedsattArbeidsevneDato = LocalDate.of(2020, 1, 1),
                        vurdertAv = "saksbehandler"
                    ),
                    yrkesskadeBeløpVurdering = BeregningYrkeskaderBeløpVurdering(
                        vurderinger = listOf(
                            YrkesskadeBeløpVurdering(
                                antattÅrligInntekt = Beløp(500000),
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
        beregning as GrunnlagYrkesskade
        beregning.underliggende() as GrunnlagUføre
        assertThat(beregning.grunnlaget()).isEqualTo(GUnit("4.6205242620"))
    }


    @Test
    fun `Hvis uføregraden er 0 prosent, endres ikke grunnlaget`() {
        val inputMedNullUføregrad = Inntektsbehov(
            Input(
                nedsettelsesDato = LocalDate.of(2023, 1, 1),
                inntekter = setOf(
                    InntektPerÅr(2022, Beløp(500000)),
                    InntektPerÅr(2021, Beløp(400000)),
                    InntektPerÅr(2020, Beløp(300000))
                ),
                uføregrad = listOf(Uføre(LocalDate.now(), Prosent(0))),
                yrkesskadevurdering = null,
                beregningGrunnlag = BeregningGrunnlag(
                    tidspunktVurdering = BeregningstidspunktVurdering(
                        begrunnelse = "test",
                        ytterligereNedsattArbeidsevneDato = LocalDate.of(2023, 1, 1),
                        nedsattArbeidsevneDato = LocalDate.of(2023, 1, 1),
                        ytterligereNedsattBegrunnelse = "test2",
                        vurdertAv = "saksbehandler"
                    ),
                    yrkesskadeBeløpVurdering = null
                ),
                registrerteYrkesskader = null
            )
        )

        val inputMedUføreGradIkkeOppgitt = Inntektsbehov(
            Input(
                nedsettelsesDato = LocalDate.of(2023, 1, 1),
                inntekter = setOf(
                    InntektPerÅr(2022, Beløp(500000)),
                    InntektPerÅr(2021, Beløp(400000)),
                    InntektPerÅr(2020, Beløp(300000))
                ),
                uføregrad = emptyList(),
                yrkesskadevurdering = null,
                beregningGrunnlag = BeregningGrunnlag(
                    tidspunktVurdering = BeregningstidspunktVurdering(
                        begrunnelse = "test",
                        ytterligereNedsattArbeidsevneDato = null,
                        nedsattArbeidsevneDato = LocalDate.of(2023, 1, 1),
                        ytterligereNedsattBegrunnelse = "asdf",
                        vurdertAv = "saksbehandler"
                    ), yrkesskadeBeløpVurdering = null
                ),
                registrerteYrkesskader = null
            )
        )

        val beregning = Beregning(inputMedNullUføregrad).beregneMedInput()
        val beregningUtenUføregrad = Beregning(inputMedUføreGradIkkeOppgitt).beregneMedInput()

        assertThat(beregning.grunnlaget()).isEqualTo(beregningUtenUføregrad.grunnlaget())
    }


}
