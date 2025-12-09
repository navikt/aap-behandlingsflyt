@file:Suppress("IDENTITY_SENSITIVE_OPERATIONS_WITH_VALUE_TYPE")

package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

class UføreBeregningTest {

    @Test
    fun `beregningen håndterer en kort periode med 100 prosent uføre, da justeres inntektene til null kroner`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = elleveNittenGrunnlag(),
            uføregrader = uføreGrader(
                1 januar 2020 to Prosent.`30_PROSENT`,
                1 januar 2021 to Prosent.`100_PROSENT`,
                1 januar 2022 to Prosent.`30_PROSENT`
            ),
            relevanteÅr = relevanteÅr(2020, 2021, 2022),
            inntektsPerioder = genererInntektsPerioder(
                2022 to 0.7 * 5 * 109_784 / 12.0,
                2021 to 0.7 * 5 * 104_716 / 12,
                2020 to 0.7 * 5 * 100_853 / 12
            )
        )

        val resultat = uføreBeregning.beregnUføre(Year.of(2023))

        val uføreInntekt2021 = resultat.uføreInntekterFraForegåendeÅr().first { it.år == Year.of(2021) }
        val uføreInntekt2022 = resultat.uføreInntekterFraForegåendeÅr().first { it.år == Year.of(2022) }

        assertThat(uføreInntekt2021.inntektJustertForUføregrad).isEqualTo(Beløp(0))
        assertThat(uføreInntekt2022.inntektJustertForUføregrad).isEqualTo(Beløp("548919.96"))
        assertThat(uføreInntekt2022.inntektIGJustertForUføregrad.verdi()).isCloseTo(
            (3.5 / 0.7).toBigDecimal(), Percentage.withPercentage(0.001)
        )
    }

    @Test
    fun `Hvis bruker hadde høyere inntekt ved ytterligere nedsatt, justert for uføregrad, brukes inntekter fra ytterligere nedsatt`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = elleveNittenGrunnlag(),
            uføregrader = uføreGrader(LocalDate.of(2017, 1, 1) to Prosent.`30_PROSENT`),
            relevanteÅr = relevanteÅr(2020, 2021, 2022),
            inntektsPerioder = genererInntektsPerioder(
                2022 to 0.7 * 5 * 109_784 / 12,
                2021 to 0.7 * 5 * 104_716 / 12,
                2020 to 0.7 * 5 * 100_853 / 12
            )
        )

        val grunnlagUføre = uføreBeregning.beregnUføre(Year.of(2023))

        assertThat(grunnlagUføre.grunnlaget().verdi()).isCloseTo(BigDecimal(5), Percentage.withPercentage(0.001))
        assertThat(grunnlagUføre.type()).isEqualTo(GrunnlagUføre.Type.YTTERLIGERE_NEDSATT)
    }

    @Test
    fun `Hvis bruker hadde lavere inntekt ved ytterligere nedsatt, justert for uføregrad, brukes inntekter fra nedsatt med halvparten`() {
        // Skal verifisere at om elleveNittenGrunnlaget er større enn uføre-beregningen, så velges elleveNittenGrunnlaget
        // som grunnlag.

        val uføreBeregning = UføreBeregning(
            grunnlag = elleveNittenGrunnlag(6),
            uføregrader = uføreGrader(LocalDate.of(2019, 7, 1) to Prosent.`30_PROSENT`),
            relevanteÅr = relevanteÅr(2020, 2021, 2022),
            inntektsPerioder = genererInntektsPerioder(
                2022 to 200_000 / 12,
                2021 to 300_000 / 12,
                2020 to 400_000 / 12
            )
        )

        val grunnlagUføre = uføreBeregning.beregnUføre(Year.of(2023))

        assertThat(grunnlagUføre.grunnlaget()).isEqualTo(GUnit(6))
        assertThat(grunnlagUføre.type()).isEqualTo(GrunnlagUføre.Type.STANDARD)
    }

    @Test
    fun `Oppjustering midt i et år skal ikke gi oppjustering for hele året`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = elleveNittenGrunnlag(2),
            uføregrader = uføreGrader(LocalDate.of(2022, 7, 1) to Prosent.`50_PROSENT`),
            relevanteÅr = relevanteÅr(2020, 2021, 2022),
            inntektsPerioder = genererInntektsPerioder(
                // Månedsinntekt på 20_000
                2022 to 20_000,
                2021 to 20_000,
                2020 to 20_000
            )
        )

        val grunnlagUføre = uføreBeregning.beregnUføre(Year.of(2023))

        assertThat(grunnlagUføre.type()).isEqualTo(GrunnlagUføre.Type.YTTERLIGERE_NEDSATT)
        val uføreInntekterFra2022 = grunnlagUføre.uføreInntekterFraForegåendeÅr().first { it.år == Year.of(2022) }

        assertThat(uføreInntekterFra2022.inntektIKroner).isEqualTo(Beløp(12 * 20000))
        assertThat(uføreInntekterFra2022.inntektJustertForUføregrad).isEqualTo(Beløp(6 * 20000 + 6 * 40000))
    }

    @Test
    fun `Oppjusterer riktig for flere uføregrader`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = elleveNittenGrunnlag(2),
            uføregrader = uføreGrader(
                LocalDate.of(2022, 7, 1) to Prosent.`50_PROSENT`,
                LocalDate.of(2021, 2, 1) to Prosent(80)
            ),
            relevanteÅr = relevanteÅr(2020, 2021, 2022),
            inntektsPerioder = genererInntektsPerioder(
                2022 to 10_000,
                2021 to 5_000,
                2020 to 20_000
            )
        )

        val grunnlagUføre = uføreBeregning.beregnUføre(Year.of(2023))

        // 6 mnd 50% uføre, 6 mnd 80% uføre
        val uføreInntekterFraForegåendeÅr = grunnlagUføre.uføreInntekterFraForegåendeÅr()
        val for2022 = uføreInntekterFraForegåendeÅr.first { it.år == Year.of(2022) }
        assertThat(for2022.inntektJustertForUføregrad)
            .isEqualTo(Beløp(6 * 20_000 + 6 * 50_000))
        assertThat(for2022.inntektIKroner.verdi.toDouble())
            .isEqualTo(12 * 10_000.0)

        // 1 mnd vanlig inntekt uten oppjustering, 11 mnd 80% uføre
        val uføreInntekter2021 = uføreInntekterFraForegåendeÅr.first { it.år == Year.of(2021) }
        assertThat(
            uføreInntekter2021.inntektJustertForUføregrad
        ).isEqualTo(Beløp(1 * 5000 + 11 * 25000))
        assertThat(grunnlagUføre.type()).isEqualTo(GrunnlagUføre.Type.YTTERLIGERE_NEDSATT)
    }

    @Test
    fun `50 prosent uføre et halvt år, 0 prosent uføre resten av året`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = elleveNittenGrunnlag(2),
            uføregrader = uføreGrader(
                LocalDate.of(2020, 1, 1) to Prosent.`50_PROSENT`,
                LocalDate.of(2022, 7, 1) to Prosent.`0_PROSENT`
            ),
            relevanteÅr = relevanteÅr(2020, 2021, 2022),
            inntektsPerioder =
                genererInntektsPerioder(
                    2022 to 10_000,
                    2021 to 15_000,
                    2020 to 20_000
                ),
        )

        val grunnlag = uføreBeregning.beregnUføre(Year.of(2023))

        val uføreInntekt2022 = grunnlag.uføreInntekterFraForegåendeÅr().first { it.år == Year.of(2022) }

        assertThat(uføreInntekt2022.inntektIKroner.verdi.toDouble()).isEqualTo(10_000 * 12.0)
        // Halve året skal oppjusteres med uføregraden
        assertThat(uføreInntekt2022.inntektJustertForUføregrad.verdi.toDouble()).isEqualTo(10_000 * 6 + 10_000 * 6 / 0.5)

    }

    private fun relevanteÅr(vararg år: Int): Set<Year> = år.map(Year::of).toSet()

    private fun uføreGrader(vararg gradering: Pair<LocalDate, Prosent>): Set<Uføre> {
        return gradering.map { (virkningstidspunkt, uføregrad) ->
            Uføre(
                virkningstidspunkt = virkningstidspunkt,
                uføregrad = uføregrad
            )
        }.toSet()
    }

    private fun elleveNittenGrunnlag(grunnlag: Int = 4): Grunnlag11_19 = Grunnlag11_19(
        grunnlaget = GUnit(grunnlag),
        erGjennomsnitt = false,
        gjennomsnittligInntektIG = GUnit(0),
        inntekter = emptyList()
    )

    private fun genererInntektsPerioder(vararg månedsInntektPerÅr: Pair<Int, Number>): Set<Månedsinntekt> {
        return månedsInntektPerÅr.toList().map { (år, beløp) -> oppsplittetInntekt(år, Beløp(beløp.toDouble().toBigDecimal())) }
            .flatten().toSet()
    }

    private fun oppsplittetInntekt(år: Int, månedsInntekt: Beløp): List<Månedsinntekt> {
        return (1..12).toList().map {
            Månedsinntekt(
                årMåned = YearMonth.of(år, it),
                beløp = månedsInntekt,
            )
        }
    }
}
