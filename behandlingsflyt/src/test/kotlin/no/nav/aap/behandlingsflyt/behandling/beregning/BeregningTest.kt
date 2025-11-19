package no.nav.aap.behandlingsflyt.behandling.beregning

import io.github.nchaugen.tabletest.junit.TableTest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.BeregningInput
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.Inntektsbehov
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningYrkeskaderBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.YrkesskadeSak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year

class BeregningTest {

    @Test
    fun `beregn input med basic 11_19 uten yrkesskade eller uføre`() {
        val input = Inntektsbehov(
            input = BeregningInput(
                nedsettelsesDato = LocalDate.of(2023, 1, 1),
                årsInntekter = setOf(
                    InntektPerÅr(2022, Beløp(500000)),
                    InntektPerÅr(2021, Beløp(400000)),
                    InntektPerÅr(2020, Beløp(300000))
                ),
                uføregrad = emptySet(),
                yrkesskadevurdering = null,
                beregningGrunnlag = null,
                registrerteYrkesskader = null,
                inntektsPerioder = listOf(
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
                        beløp = 500000.toDouble(),
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
                        beløp = 400000.toDouble(),
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)),
                        beløp = 300000.toDouble(),
                    ),
                )
            )
        )

        val beregning = Beregning(input).beregneMedInput()

        assertThat(beregning.grunnlaget()).isEqualTo(GUnit("4.5543977264"))
    }

    @Test
    fun `oppjusterer grunnlaget ved uføre`() {
        val input = Inntektsbehov(
            BeregningInput(
                nedsettelsesDato = LocalDate.of(2015, 1, 1),
                årsInntekter = setOf(
                    InntektPerÅr(2022, Beløp(500000)),
                    InntektPerÅr(2021, Beløp(400000)),
                    InntektPerÅr(2020, Beløp(300000))
                ),
                uføregrad = setOf(Uføre(LocalDate.now().minusYears(5), Prosent(30))),
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
                registrerteYrkesskader = null,
                inntektsPerioder = listOf(
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
                        beløp = 500000.toDouble(),
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
                        beløp = 400000.toDouble(),
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)),
                        beløp = 300000.toDouble(),
                    ),
                )
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
            BeregningInput(
                nedsettelsesDato = LocalDate.of(2023, 1, 1),
                årsInntekter = inntekterPerÅr,
                uføregrad = emptySet(),
                yrkesskadevurdering = Yrkesskadevurdering(
                    begrunnelse = "en begrunnelse",
                    andelAvNedsettelsen = Prosent(40),
                    erÅrsakssammenheng = true,
                    relevanteSaker = listOf(YrkesskadeSak("yrkesskadesaken", null)),
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
                                begrunnelse = "asdf",
                                vurdertAv = "saksbehandler"
                            )
                        )
                    )
                ),
                registrerteYrkesskader = Yrkesskader(
                    listOf(
                        Yrkesskade(
                            ref = "yrkesskadesaken",
                            saksnummer = 123,
                            kildesystem = "INFOTRYGD",
                            skadedato = LocalDate.of(2019, 1, 1)
                        )
                    )
                ),
                inntektsPerioder = listOf(
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
                        beløp = 500000.toDouble(),
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
                        beløp = 400000.toDouble(),
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)),
                        beløp = 300000.toDouble(),
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
            BeregningInput(
                nedsettelsesDato = LocalDate.of(2023, 1, 1),
                årsInntekter = setOf(
                    InntektPerÅr(2022, Beløp(500000)),
                    InntektPerÅr(2021, Beløp(400000)),
                    InntektPerÅr(2020, Beløp(300000))
                ),
                uføregrad = setOf(Uføre(LocalDate.now(), Prosent(50))),
                yrkesskadevurdering = Yrkesskadevurdering(
                    begrunnelse = "en begrunnelse",
                    andelAvNedsettelsen = Prosent(30),
                    erÅrsakssammenheng = true,
                    relevanteSaker = listOf(YrkesskadeSak("yrkesskadesaken", null)),
                    vurdertAv = "saksbehandler"
                ),
                beregningGrunnlag = BeregningGrunnlag(
                    tidspunktVurdering = BeregningstidspunktVurdering(
                        begrunnelse = "test",
                        nedsattArbeidsevneDato = LocalDate.of(2023, 1, 1),
                        ytterligereNedsattBegrunnelse = "test2",
                        ytterligereNedsattArbeidsevneDato = LocalDate.of(2023, 1, 1),
                        vurdertAv = "saksbehandler"
                    ),
                    yrkesskadeBeløpVurdering = BeregningYrkeskaderBeløpVurdering(
                        vurderinger = listOf(
                            YrkesskadeBeløpVurdering(
                                antattÅrligInntekt = Beløp(500000),
                                referanse = "yrkesskadesaken",
                                begrunnelse = "asdf",
                                vurdertAv = "saksbehandler"
                            )
                        )
                    )
                ),
                registrerteYrkesskader = Yrkesskader(
                    listOf(
                        Yrkesskade(
                            ref = "yrkesskadesaken",
                            saksnummer = 123,
                            kildesystem = "INFOTRYGD",
                            skadedato = LocalDate.of(2021, 1, 1)
                        )
                    )
                ),
                inntektsPerioder = listOf(
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
                        beløp = 500000.toDouble(),
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
                        beløp = 400000.toDouble(),
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)),
                        beløp = 300000.toDouble(),
                    ),
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
            BeregningInput(
                nedsettelsesDato = LocalDate.of(2023, 1, 1),
                årsInntekter = setOf(
                    InntektPerÅr(2022, Beløp(500000)),
                    InntektPerÅr(2021, Beløp(400000)),
                    InntektPerÅr(2020, Beløp(300000))
                ),
                uføregrad = setOf(Uføre(LocalDate.now(), Prosent(0))),
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
                registrerteYrkesskader = null,
                inntektsPerioder = listOf(
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
                        beløp = 500000.toDouble(),
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
                        beløp = 400000.toDouble(),
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)),
                        beløp = 300000.toDouble(),
                    ),

                    )
            )
        )

        val inputMedUføreGradIkkeOppgitt = Inntektsbehov(
            BeregningInput(
                nedsettelsesDato = LocalDate.of(2023, 1, 1),
                årsInntekter = setOf(
                    InntektPerÅr(2022, Beløp(500000)),
                    InntektPerÅr(2021, Beløp(400000)),
                    InntektPerÅr(2020, Beløp(300000))
                ),
                uføregrad = setOf(),
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
                registrerteYrkesskader = null,
                inntektsPerioder = listOf(
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
                        beløp = 500000.toDouble(),
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
                        beløp = 400000.toDouble(),
                    ),
                    InntektsPeriode(
                        periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)),
                        beløp = 300000.toDouble(),
                    )
                )
            )
        )

        val beregning = Beregning(inputMedNullUføregrad).beregneMedInput()
        val beregningUtenUføregrad = Beregning(inputMedUføreGradIkkeOppgitt).beregneMedInput()

        assertThat(beregning.grunnlaget()).isEqualTo(beregningUtenUføregrad.grunnlaget())
    }

    @TableTest(
        """    
        Scenario                                   | nedsettelsesÅr | inntektPerÅr                 | ForventetG
        Enkel 11-19                                | 2023           | [2020: 3, 2021: 4, 2022: 5]  | 5
        Velg gjennomsnitt hvis høyere              | 2023           | [2020: 5, 2021: 5, 2022: 2]  | 4
        Begrens til 6G om siste år er høyt         | 2023           | [2020: 3, 2021: 3, 2022: 10] | 6
        Begrens til 6G om gjennomsnitt høyt        | 2023           | [2020: 7, 2021: 7, 2022: 7]  | 6
        Om vi mangler inntekter, blir grunnlaget 0 | 2023           | [2024: 0]                    | 0
        """
    )
    fun `11-19 tabelldrevet test`(nedsettelsesÅr: Year, inntektPerÅr: Set<InntektPerÅr>, forventetGrunnlag: GUnit) {
        val nedsettelsesDato = nedsettelsesÅr.atDay(1)
        val input = Inntektsbehov(
            input = BeregningInput(
                nedsettelsesDato = nedsettelsesDato,
                uføregrad = emptySet(),
                yrkesskadevurdering = null,
                beregningGrunnlag = null,
                registrerteYrkesskader = null,
                årsInntekter = TODO(),
                inntektsPerioder = TODO()
            )
        )
        val beregning = Beregning(input).beregneMedInput()
        val actual = beregning.grunnlaget()

        assertThat(actual).isEqualTo(forventetGrunnlag)
    }

    @Suppress("unused")
    companion object {
        @JvmStatic
        fun parseInntektPerÅr(map: Map<String, Double>): Set<InntektPerÅr> {
            return map.entries.map { (år, gVerdi) ->
                val g =
                    Grunnbeløp.tilTidslinjeGjennomsnitt().segment(Year.of(år.toInt()).atDay(250))?.verdi!!.multiplisert(
                        GUnit(gVerdi.toString())
                    )
                InntektPerÅr(år.toInt(), g)
            }.toSet()
        }

        @JvmStatic
        fun parseGrunnlag(verdi: Double): GUnit {
            return GUnit(BigDecimal(verdi.toString()))
        }
    }

}
