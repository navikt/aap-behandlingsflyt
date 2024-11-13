package no.nav.aap.tidslinje

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TidslinjeTest {

    @Test
    fun `skal lage tidslinje med verdier`() {
        val firstSegment = Segment(Periode(LocalDate.now(), LocalDate.now().plusDays(10)), Beløp(100))
        val secondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)), Beløp(200))
        val tidslinje = Tidslinje(
            listOf(
                firstSegment,
                secondSegment
            )
        )

        assertThat(tidslinje.segmenter()).containsExactly(secondSegment, firstSegment)
    }

    @Test
    fun `skal slå sammen perioder med lik verdi ved compress`() {
        val firstSegment = Segment(Periode(LocalDate.now(), LocalDate.now().plusDays(10)), Beløp(100))
        val secondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)), Beløp(100))
        val tidslinje = Tidslinje(
            listOf(
                firstSegment,
                secondSegment
            )
        )

        assertThat(tidslinje.segmenter()).containsExactly(secondSegment, firstSegment)
        assertThat(tidslinje.komprimer().segmenter()).containsExactly(
            Segment(
                Periode(
                    secondSegment.periode.fom,
                    firstSegment.periode.tom
                ), Beløp(100)
            )
        )
        // Bare så det er tydelig at compress ikke gjør inline manipulasjon
        assertThat(tidslinje.segmenter()).containsExactly(secondSegment, firstSegment)
    }

    @Test
    fun `skal slå sammen to tidslinjer`() {
        val firstSegment = Segment(Periode(LocalDate.now(), LocalDate.now().plusDays(10)), Beløp(100))
        val secondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)), Beløp(200))
        val tidslinje = Tidslinje(listOf(firstSegment))
        val tidslinje1 = Tidslinje(listOf(secondSegment))

        val mergetTidslinje = tidslinje.kombiner(
            tidslinje1,
            StandardSammenslåere.prioriterHøyreSideCrossJoin()
        )

        assertThat(mergetTidslinje.segmenter()).containsExactly(secondSegment, firstSegment)
    }

    @Test
    fun `skal slå sammen to tidslinjer med overlapp`() {
        val firstSegment = Segment(Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(10)), Beløp(100))
        val expectedFirstSegment = Segment(Periode(LocalDate.now(), LocalDate.now().plusDays(10)), Beløp(100))
        val secondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)), Beløp(200))
        val tidslinje = Tidslinje(listOf(firstSegment))
        val tidslinje1 = Tidslinje(listOf(secondSegment))

        val mergetTidslinje = tidslinje.kombiner(
            tidslinje1,
            StandardSammenslåere.prioriterHøyreSideCrossJoin()
        ).komprimer()

        assertThat(mergetTidslinje.segmenter()).containsExactly(secondSegment, expectedFirstSegment)
    }

    @Test
    fun `skal slå sammen to tidslinjer med overlapp med custom sammenslåer`() {
        val firstSegment = Segment(Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(10)), Beløp(100))
        val secondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)), Beløp(200))
        val tidslinje = Tidslinje(listOf(firstSegment))
        val tidslinje1 = Tidslinje(listOf(secondSegment))

        val mergetTidslinje: Tidslinje<Beløp> =
            tidslinje.kombiner(tidslinje1, StandardSammenslåere.summerer()).komprimer()

        assertThat(mergetTidslinje.segmenter()).containsExactly(
            Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(3)), Beløp(200)),
            Segment(Periode(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1)), Beløp(300)),
            Segment(Periode(LocalDate.now(), LocalDate.now().plusDays(10)), Beløp(100))
        )
    }

    @Test
    fun `skal kunne styre prioritet mellom tidslinjer`() {
        val firstSegment = Segment(Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(10)), Beløp(100))
        val secondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)), Beløp(200))
        val expectedSecondSegment =
            Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(3)), Beløp(200))
        val tidslinje = Tidslinje(listOf(firstSegment))
        val tidslinje1 = Tidslinje(listOf(secondSegment))

        val mergetTidslinje =
            tidslinje.kombiner(tidslinje1, StandardSammenslåere.prioriterVenstreSideCrossJoin())
                .komprimer()

        assertThat(mergetTidslinje.segmenter()).containsExactly(expectedSecondSegment, firstSegment)
    }

    @Test
    fun `slå sammen ulike typer`() {
        val fullPeriode = Periode(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10))
        val delPeriode1 = Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(6))
        val delPeriode2 = Periode(LocalDate.now().minusDays(5), LocalDate.now())
        val delPeriode3 = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(10))

        val beløp = Beløp(756)
        val firstSegment = Segment(fullPeriode, beløp)

        val tidslinje = Tidslinje(listOf(firstSegment))
        val tidslinje1 = Tidslinje(
            listOf(
                Segment(delPeriode1, Prosent(10)),
                Segment(delPeriode2, Prosent(50)),
                Segment(delPeriode3, Prosent(78))
            )
        )

        val mergetTidslinje = tidslinje.kombiner(tidslinje1, UtregningSammenslåer()).komprimer()

        assertThat(mergetTidslinje.segmenter()).containsExactly(
            Segment(delPeriode1, Utbetaling(beløp, Prosent(10))),
            Segment(delPeriode2, Utbetaling(beløp, Prosent(50))),
            Segment(delPeriode3, Utbetaling(beløp, Prosent(78)))
        )
    }

    @Test
    fun `skal slå sammen to tidslinjer med overlapp, frittstående elementer i en av tidslinjene og hull`() {
        val fullPeriode = Periode(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10))
        val delPeriode1 = Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(7))
        val delPeriode2 = Periode(LocalDate.now().minusDays(5), LocalDate.now())
        val delPeriode3 = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(10))
        val delPeriode4 = Periode(LocalDate.now().plusDays(15), LocalDate.now().plusDays(20))

        val beløp = Beløp(756)
        val firstSegment = Segment(fullPeriode, beløp)

        val tidslinje = Tidslinje(listOf(firstSegment))
        val tidslinje1 = Tidslinje(
            listOf(
                Segment(delPeriode1, Beløp(10)),
                Segment(delPeriode2, Beløp(50)),
                Segment(delPeriode3, Beløp(78)),
                Segment(delPeriode4, Beløp(99))
            )
        )

        val mergetTidslinje: Tidslinje<Beløp> =
            tidslinje.kombiner(tidslinje1, StandardSammenslåere.prioriterHøyreSideCrossJoin()).komprimer()

        assertThat(mergetTidslinje.segmenter()).containsExactly(
            Segment(delPeriode1, Beløp(10)),
            Segment(Periode(LocalDate.now().minusDays(6), LocalDate.now().minusDays(6)), beløp),
            Segment(delPeriode2, Beløp(50)),
            Segment(delPeriode3, Beløp(78)),
            Segment(delPeriode4, Beløp(99))
        )
    }

    @Test
    fun `enkel test med barnetilegg`() {
        val fullPeriode = Periode(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10))

        val delPeriode1 = Periode(LocalDate.now().minusDays(10), LocalDate.now())
        val delPeriode2 = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(10))

        val beløp = Beløp(756)
        val firstSegment = Segment(fullPeriode, beløp)

        val grunnlagTidslinje = Tidslinje(listOf(firstSegment))
        val barnetileggSats = Tidslinje(listOf(Segment(fullPeriode, Beløp(36))))
        val antallBarnTidslinje = Tidslinje(
            listOf(
                Segment(Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(10)), 1)
            )
        )
        val uttakTidslinje = Tidslinje(
            listOf(
                Segment(delPeriode1, Prosent(67)),
                Segment(delPeriode2, Prosent(73)),
            )
        )

        val barneUtreningTidslinje = antallBarnTidslinje.kombiner(barnetileggSats, BarneTileggUtbetaling())
        val komplettTidslinje =
            grunnlagTidslinje.kombiner(uttakTidslinje, UtregningSammenslåer())
                .kombiner(barneUtreningTidslinje, KombinertUtbetaling())

        assertThat(komplettTidslinje.segmenter()).hasSize(3)
    }
}

data class Utbetaling(val beløp: Beløp, val prosent: Prosent) {
    fun beløp(): Beløp {
        return prosent.multiplisert(beløp)
    }

    override fun toString(): String {
        return "Utbetaling(beløp=$beløp, prosent=$prosent, utbetaling=${beløp()})"
    }
}

data class UtbetalingMedBarneTilegg(val beløp: Beløp, val barnetilegg: Beløp, val prosent: Prosent) {
    fun beløp(): Beløp {
        return prosent.multiplisert(beløp.pluss(barnetilegg))
    }

    override fun toString(): String {
        return "Utbetaling(beløp=$beløp, barnetilegg=$barnetilegg, prosent=$prosent, utbetaling=${beløp()})"
    }
}

class BarneTileggUtbetaling :
    JoinStyle<Int, Beløp, Beløp> by JoinStyle.OUTER_JOIN(
        { periode: Periode, venstreSegment, høyreSegment ->
            val prosent = venstreSegment?.verdi ?: 0
            val beløp = høyreSegment?.verdi ?: Beløp(0)
            Segment(periode, beløp.multiplisert(prosent))
        })

class KombinertUtbetaling :
    JoinStyle<Utbetaling, Beløp, UtbetalingMedBarneTilegg> by JoinStyle.OUTER_JOIN(
        { periode: Periode, venstreSegment, høyreSegment ->
            if (venstreSegment == null) {
                return@OUTER_JOIN null
            }
            val beløp = høyreSegment?.verdi ?: Beløp(0)
            Segment(periode, UtbetalingMedBarneTilegg(venstreSegment.verdi.beløp, beløp, venstreSegment.verdi.prosent))
        })

class UtregningSammenslåer :
    JoinStyle<Beløp, Prosent, Utbetaling> by JoinStyle.OUTER_JOIN(
        { periode: Periode, venstreSegment, høyreSegment ->
            val beløp = venstreSegment?.verdi ?: Beløp(0)
            val prosent = høyreSegment?.verdi ?: Prosent(0)
            Segment(periode, Utbetaling(beløp, prosent))
        }
    )
