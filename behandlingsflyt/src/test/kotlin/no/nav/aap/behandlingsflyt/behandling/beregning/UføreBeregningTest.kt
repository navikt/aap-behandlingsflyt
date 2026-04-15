package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

class UføreBeregningTest {

    @Test
    fun `beregningen håndterer en kort periode med 100 prosent uføre, da justeres inntektene til null kroner`() {
        val (inntektsPerioder, årsInntekter) = genererInntektsPerioder(
            2022 to 0.7 * 5 * 109_784 / 12.0,
            2021 to 0.7 * 5 * 104_716 / 12,
            2020 to 0.7 * 5 * 100_853 / 12
        )
        val uføreBeregning = UføreBeregning(
            grunnlag = elleveNittenGrunnlag(),
            uføregrader = uføreGrader(
                1 januar 2020 to Prosent.`30_PROSENT`,
                1 januar 2021 to Prosent.`100_PROSENT`,
                1 januar 2022 to Prosent.`30_PROSENT`
            ),
            inntektsPerioder = inntektsPerioder,
            ytterligereNedsattDato = LocalDate.of(2023, 10, 1),
            årsInntekter = årsInntekter
        )

        val resultat = uføreBeregning.beregnUføre()

        val uføreInntekt2021 = resultat.uføreInntekterFraForegåendeÅr().first { it.år == Year.of(2021) }
        val uføreInntekt2022 = resultat.uføreInntekterFraForegåendeÅr().first { it.år == Year.of(2022) }

        assertThat(uføreInntekt2021.inntektJustertForUføregrad).isEqualTo(Beløp(0))
        assertThat(uføreInntekt2022.inntektJustertForUføregrad).isEqualTo(Beløp("548919.94"))
        assertThat(uføreInntekt2022.inntektIGJustertForUføregrad.verdi()).isCloseTo(
            (3.5 / 0.7).toBigDecimal(), Percentage.withPercentage(0.001)
        )
    }

    @Test
    fun `Hvis bruker hadde høyere inntekt ved ytterligere nedsatt, justert for uføregrad, brukes inntekter fra ytterligere nedsatt`() {
        val (inntektsPerioder, årsInntekter) = genererInntektsPerioder(
            2022 to 0.7 * 5 * 109_784 / 12,
            2021 to 0.7 * 5 * 104_716 / 12,
            2020 to 0.7 * 5 * 100_853 / 12
        )
        val uføreBeregning = UføreBeregning(
            grunnlag = elleveNittenGrunnlag(),
            uføregrader = uføreGrader(LocalDate.of(2017, 1, 1) to Prosent.`30_PROSENT`),
            inntektsPerioder = inntektsPerioder,
            ytterligereNedsattDato = LocalDate.of(2023, 1, 1),
            årsInntekter = årsInntekter,
        )

        val grunnlagUføre = uføreBeregning.beregnUføre()

        assertThat(grunnlagUføre.grunnlaget().verdi()).isCloseTo(BigDecimal(5), Percentage.withPercentage(0.001))
        assertThat(grunnlagUføre.type()).isEqualTo(GrunnlagUføre.Type.YTTERLIGERE_NEDSATT)
    }

    @Test
    fun `Hvis bruker hadde lavere inntekt ved ytterligere nedsatt, justert for uføregrad, brukes inntekter fra nedsatt med halvparten`() {
        // Skal verifisere at om elleveNittenGrunnlaget er større enn uføre-beregningen, så velges elleveNittenGrunnlaget
        // som grunnlag.

        val (inntektsPerioder, årsInntekter) = genererInntektsPerioder(
            2022 to 200_000 / 12,
            2021 to 300_000 / 12,
            2020 to 400_000 / 12
        )
        val uføreBeregning = UføreBeregning(
            grunnlag = elleveNittenGrunnlag(6),
            uføregrader = uføreGrader(LocalDate.of(2019, 7, 1) to Prosent.`30_PROSENT`),
            inntektsPerioder = inntektsPerioder,
            ytterligereNedsattDato = LocalDate.of(2023, 1, 1),
            årsInntekter = årsInntekter,
        )

        val grunnlagUføre = uføreBeregning.beregnUføre()

        assertThat(grunnlagUføre.grunnlaget()).isEqualTo(GUnit(6))
        assertThat(grunnlagUføre.type()).isEqualTo(GrunnlagUføre.Type.STANDARD)
    }

    @Test
    fun `Oppjustering midt i et år skal ikke gi oppjustering for hele året`() {
        val (inntektsPerioder, årsInntekter) = genererInntektsPerioder(
            // Månedsinntekt på 20_000
            2022 to 20_000,
            2021 to 20_000,
            2020 to 20_000
        )
        val uføreBeregning = UføreBeregning(
            grunnlag = elleveNittenGrunnlag(2),
            uføregrader = uføreGrader(LocalDate.of(2022, 7, 1) to Prosent.`50_PROSENT`),
            inntektsPerioder = inntektsPerioder,
            ytterligereNedsattDato = LocalDate.of(2023, 8, 4),
            årsInntekter = årsInntekter,
        )

        val grunnlagUføre = uføreBeregning.beregnUføre()

        assertThat(grunnlagUføre.type()).isEqualTo(GrunnlagUføre.Type.YTTERLIGERE_NEDSATT)
        val uføreInntekterFra2022 = grunnlagUføre.uføreInntekterFraForegåendeÅr().first { it.år == Year.of(2022) }

        assertThat(uføreInntekterFra2022.inntektIKroner).isEqualTo(Beløp(12 * 20000))
        assertThat(uføreInntekterFra2022.inntektJustertForUføregrad).isEqualTo(Beløp(6 * 20000 + 6 * 40000))
    }

    @Test
    fun `Oppjusterer riktig for flere uføregrader`() {
        val (inntektsPerioder, årsInntekter) = genererInntektsPerioder(
            2022 to 10_000,
            2021 to 5_000,
            2020 to 20_000
        )
        val uføreBeregning = UføreBeregning(
            grunnlag = elleveNittenGrunnlag(2),
            uføregrader = uføreGrader(
                LocalDate.of(2022, 7, 1) to Prosent.`50_PROSENT`,
                LocalDate.of(2021, 2, 1) to Prosent(80)
            ),
            inntektsPerioder = inntektsPerioder,
            ytterligereNedsattDato = LocalDate.of(2023, 4, 2),
            årsInntekter = årsInntekter,
        )

        val grunnlagUføre = uføreBeregning.beregnUføre()

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
    fun `hvis uføregraden er konstant i relevante år, brukes årsinntekt`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = elleveNittenGrunnlag(2),
            uføregrader = uføreGrader(
                LocalDate.of(2022, 1, 1) to Prosent.`50_PROSENT`,
            ),
            // Ingen månedsinntekter
            inntektsPerioder = emptySet(),
            ytterligereNedsattDato = LocalDate.of(2023, 4, 2),
            årsInntekter = setOf(
                InntektPerÅr(2021, Beløp(10_000)),
                InntektPerÅr(2020, Beløp(10_000)),
                InntektPerÅr(2022, Beløp(10_000))
            ),
        )

        val grunnlagUføre = uføreBeregning.beregnUføre()

        grunnlagUføre.uføreInntekterFraForegåendeÅr().forEach { inntekt ->
            assertThat(inntekt.inntektsPerioder).hasSize(1)
            assertThat(inntekt.inntektIKroner.verdi).isGreaterThan(BigDecimal.ZERO)
            assertThat(inntekt.inntektsPerioder.first().inntektIKroner).isEqualTo(inntekt.inntektIKroner)
        }
    }

    @Test
    fun `hvis uføregraden er forskjellig midt i et år, og månedsinntekt og årsinntekt er forskjellig, så krasjer det`() {
        val uføreBeregning = UføreBeregning(
            grunnlag = elleveNittenGrunnlag(2),
            uføregrader = uføreGrader(
                LocalDate.of(2021, 1, 1) to Prosent.`50_PROSENT`,
                LocalDate.of(2021, 6, 1) to Prosent.`66_PROSENT`,
            ),
            // Ingen månedsinntekter
            inntektsPerioder = emptySet(),
            ytterligereNedsattDato = LocalDate.of(2023, 4, 2),
            årsInntekter = setOf(
                InntektPerÅr(2021, Beløp(10_000)),
                InntektPerÅr(2020, Beløp(10_000)),
                InntektPerÅr(2022, Beløp(10_000))
            ),
        )

        assertThrows<IllegalArgumentException> { uføreBeregning.beregnUføre() }
    }

    @Test
    fun `50 prosent uføre et halvt år, 0 prosent uføre resten av året`() {
        val (inntektsPerioder, årsInntekter) = genererInntektsPerioder(
            2022 to 10_000,
            2021 to 15_000,
            2020 to 20_000
        )
        val uføreBeregning = UføreBeregning(
            grunnlag = elleveNittenGrunnlag(2),
            uføregrader = uføreGrader(
                LocalDate.of(2020, 1, 1) to Prosent.`50_PROSENT`,
                LocalDate.of(2022, 7, 1) to Prosent.`0_PROSENT`
            ),
            inntektsPerioder =
                inntektsPerioder,
            ytterligereNedsattDato = LocalDate.of(2023, 4, 9),
            årsInntekter = årsInntekter,
        )

        val grunnlag = uføreBeregning.beregnUføre()

        val uføreInntekt2022 = grunnlag.uføreInntekterFraForegåendeÅr().first { it.år == Year.of(2022) }

        assertThat(uføreInntekt2022.inntektIKroner.verdi.toDouble()).isEqualTo(10_000 * 12.0)
        // Halve året skal oppjusteres med uføregraden
        assertThat(uføreInntekt2022.inntektJustertForUføregrad.verdi.toDouble()).isEqualTo(10_000 * 6 + 10_000 * 6 / 0.5)
    }

    @Test
    fun `manglende inntekt fra A-inntekt tolkes som null kroner`() {
        // https://nav-it.slack.com/archives/C08PX5Z14ER/p1770790411910569?thread_ts=1770729919.721449&cid=C08PX5Z14ER
        val uføreBeregning = UføreBeregning(
            grunnlag = elleveNittenGrunnlag(0),
            uføregrader = uføreGrader(
                LocalDate.of(2020, 1, 1) to Prosent.`0_PROSENT`,
                LocalDate.of(2022, 7, 1) to Prosent.`50_PROSENT`
            ),
            inntektsPerioder = emptySet(),
            ytterligereNedsattDato = LocalDate.of(2023, 8, 4),
            årsInntekter = emptySet(),
        )

        val grunnlag = uføreBeregning.beregnUføre()

        assertThat(grunnlag.grunnlaget()).isEqualTo(GUnit(0))

        val uføreInntekt2022 = grunnlag.uføreInntekterFraForegåendeÅr().first { it.år == Year.of(2022) }

        assertThat(uføreInntekt2022.inntektIKroner.verdi.toDouble()).isEqualTo(0.0)
        // Halve året skal oppjusteres med uføregraden
        assertThat(uføreInntekt2022.inntektJustertForUføregrad.verdi.toDouble()).isEqualTo(0.0)
    }

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

    private fun genererInntektsPerioder(vararg månedsInntektPerÅr: Pair<Int, Number>): Pair<Set<Månedsinntekt>, Set<InntektPerÅr>> {
        val månedsinntekter = månedsInntektPerÅr.toList()
            .flatMap { (år, beløp) -> oppsplittetInntekt(år, Beløp(beløp.toDouble().toBigDecimal())) }.toSet()
        val årsinntekter = månedsinntekter.groupBy { it.årMåned.year }.mapValues { (år, inntekter) ->
            InntektPerÅr(år, Beløp(inntekter.sumOf { it.beløp.verdi }))
        }.map { it.value }.toSet()
        return Pair(månedsinntekter, årsinntekter)
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
