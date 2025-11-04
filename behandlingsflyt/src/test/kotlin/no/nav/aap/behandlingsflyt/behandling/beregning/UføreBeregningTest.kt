package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

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
                uføregrader = setOf(
                    Uføre(
                        virkningstidspunkt = LocalDate.now().minusYears(1),
                        uføregrad = Prosent.`100_PROSENT`,
                        kilde = "PESYS"
                    )
                ),
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
                ),
                inntektsPerioder = listOf(
                    InntektsPeriode(
                        periode = Periode(
                            fom = LocalDate.parse("2024-01-01"),
                            tom = LocalDate.parse("2024-12-31"),
                        ),
                        beløp = BigDecimal(5 * 109_784).toDouble(), // 548 920
                        inntektType = "lønn"
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
            uføregrader = setOf(Uføre(
                virkningstidspunkt = LocalDate.now().minusYears(8),
                uføregrad = Prosent.`30_PROSENT`
            )),
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
            ),
            inntektsPerioder = listOf(
                InntektsPeriode(
                    periode = Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
                    beløp = BigDecimal(5 * 109_784).multiply(BigDecimal("0.7")).toDouble(),
                    inntektType = "lønn"
                ),
                InntektsPeriode(
                    periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
                    beløp = BigDecimal(5 * 104_716).multiply(BigDecimal("0.7")).toDouble(),
                    inntektType = "lønn"
                ),
                InntektsPeriode(
                    periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)),
                    beløp = BigDecimal(5 * 100_853).multiply(BigDecimal("0.7")).toDouble(),
                    inntektType = "lønn"
                ),
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
            uføregrader = setOf(Uføre(
                virkningstidspunkt = LocalDate.now().minusYears(1),
                uføregrad = Prosent.`30_PROSENT`
            )),
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
            ),
            inntektsPerioder = listOf(
                InntektsPeriode(
                    periode = Periode(LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31)),
                    beløp = 500000.toDouble(),
                    inntektType = "lønn"
                ),
                InntektsPeriode(
                    periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)),
                    beløp = 400000.toDouble(),
                    inntektType = "lønn"
                ),
                InntektsPeriode(
                    periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)),
                    beløp = 300000.toDouble(),
                    inntektType = "lønn"
                ),

                )
        )

        val grunnlagUføre = uføreBeregning.beregnUføre(Year.of(2023))

        assertThat(grunnlagUføre.grunnlaget()).isEqualTo(GUnit(5))
        assertThat(grunnlagUføre.type()).isEqualTo(GrunnlagUføre.Type.STANDARD)
    }

    @Test
    fun `Oppjustering midt i et år skal ikke gi oppjustering for hele året`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = Grunnlag11_19(
                grunnlaget = GUnit(2),
                erGjennomsnitt = false,
                gjennomsnittligInntektIG = GUnit(0),
                inntekter = emptyList()
            ),
            uføregrader = setOf(Uføre(
                virkningstidspunkt = LocalDate.of(2022, 7, 1),
                uføregrad = Prosent.`50_PROSENT`
            )),
            inntekterForegåendeÅr = setOf(
                InntektPerÅr(
                    Year.of(2022),
                    Beløp(BigDecimal(12 * 20000))
                ),
                InntektPerÅr(
                    Year.of(2021),
                    Beløp(BigDecimal(12 * 20000))
                ),
                InntektPerÅr(
                    Year.of(2020),
                    Beløp(BigDecimal(12 * 20000))
                )
            ),
            inntektsPerioder = listOf(
                oppsplittetInntekt(2022, 20000.toDouble()),
                oppsplittetInntekt(2021, 20000.toDouble()),
                oppsplittetInntekt(2020, 20000.toDouble())
            ).flatten()
        )

        val grunnlagUføre = uføreBeregning.beregnUføre(Year.of(2023))

        assertThat(
            grunnlagUføre.uføreInntekterFraForegåendeÅr().first { it.år == Year.of(2022) }.inntektIKroner
        ).isEqualTo(Beløp(12 * 20000))
        assertThat(
            grunnlagUføre.uføreInntekterFraForegåendeÅr().first { it.år == Year.of(2022) }.inntektJustertForUføregrad
        ).isEqualTo(Beløp(6 * 20000 + 6 * 40000))

        assertThat(grunnlagUføre.type()).isEqualTo(GrunnlagUføre.Type.YTTERLIGERE_NEDSATT)
    }

    @Test
    fun `Oppjusterer riktig for flere uføregrader`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = Grunnlag11_19(
                grunnlaget = GUnit(2),
                erGjennomsnitt = false,
                gjennomsnittligInntektIG = GUnit(0),
                inntekter = emptyList()
            ),
            uføregrader = setOf(
                Uføre(
                    virkningstidspunkt = LocalDate.of(2022, 7, 1),
                    uføregrad = Prosent.`50_PROSENT`
                ),
                Uføre(
                    virkningstidspunkt = LocalDate.of(2021, 2, 1),
                    uføregrad = Prosent(80)
                )
            ),
            inntekterForegåendeÅr = setOf(
                InntektPerÅr(
                    Year.of(2022),
                    Beløp(BigDecimal(12 * 20000))
                ),
                InntektPerÅr(
                    Year.of(2021),
                    Beløp(BigDecimal(12 * 20000))
                ),
                InntektPerÅr(
                    Year.of(2020),
                    Beløp(BigDecimal(12 * 20000))
                )
            ),
            inntektsPerioder = listOf(oppsplittetInntekt(2022, 10000.toDouble()), oppsplittetInntekt(2021, 5000.toDouble()), oppsplittetInntekt(2020, 20000.toDouble())).flatten()
        )

        val grunnlagUføre = uføreBeregning.beregnUføre(Year.of(2023))

        // 6 mnd 50% uføre, 6 mnd 80% uføre
        assertThat(grunnlagUføre.uføreInntekterFraForegåendeÅr().first { it.år == Year.of(2022) }.inntektJustertForUføregrad).isEqualTo(Beløp(6 * 20000 + 6 * 50000))

        // 1 mnd vanlig inntekt uten oppjustering, 11 mnd 80% uføre
        assertThat(grunnlagUføre.uføreInntekterFraForegåendeÅr().first { it.år == Year.of(2021) }.inntektJustertForUføregrad).isEqualTo(Beløp(1 * 5000 + 11 * 25000))
        assertThat(grunnlagUføre.type()).isEqualTo(GrunnlagUføre.Type.YTTERLIGERE_NEDSATT)
    }

    private fun oppsplittetInntekt(år: Int, månedsInntekt: Double): List<InntektsPeriode> {
        return (1..12).toList().map {
            InntektsPeriode(
                periode = Periode(YearMonth.of(år, it).atDay(1), YearMonth.of(år, it).atEndOfMonth()),
                beløp = månedsInntekt,
                inntektType = "lønn"
            )
        }
    }
}
