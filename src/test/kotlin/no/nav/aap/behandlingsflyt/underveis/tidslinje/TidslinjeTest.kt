package no.nav.aap.behandlingsflyt.underveis.tidslinje

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.beregning.Prosent
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.Beløp
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

        val mergetTidslinje = tidslinje.kombiner(tidslinje1, PrioriterHøyreSide())

        assertThat(mergetTidslinje.segmenter()).containsExactly(secondSegment, firstSegment)
    }

    @Test
    fun `skal slå sammen to tidslinjer med overlapp`() {
        val firstSegment = Segment(Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(10)), Beløp(100))
        val expectedFirstSegment = Segment(Periode(LocalDate.now(), LocalDate.now().plusDays(10)), Beløp(100))
        val secondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)), Beløp(200))
        val tidslinje = Tidslinje(listOf(firstSegment))
        val tidslinje1 = Tidslinje(listOf(secondSegment))

        val mergetTidslinje: Tidslinje<Beløp> = tidslinje.kombiner(tidslinje1, PrioriterHøyreSide()).komprimer()

        assertThat(mergetTidslinje.segmenter()).containsExactly(secondSegment, expectedFirstSegment)
    }

    @Test
    fun `skal slå sammen to tidslinjer med overlapp med custom sammenslåer`() {
        val firstSegment = Segment(Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(10)), Beløp(100))
        val secondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)), Beløp(200))
        val tidslinje = Tidslinje(listOf(firstSegment))
        val tidslinje1 = Tidslinje(listOf(secondSegment))

        val mergetTidslinje: Tidslinje<Beløp> = tidslinje.kombiner(tidslinje1, Summer()).komprimer()

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

        val mergetTidslinje = tidslinje.kombiner(tidslinje1, PrioriteVenstreSide()).komprimer()

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
}

data class Utbetaling(val beløp: Beløp, val prosent: Prosent) {
    fun beløp(): Beløp {
        return Beløp(prosent.multiplisert(beløp.verdi()))
    }

    override fun toString(): String {
        return "Utbetaling(beløp=$beløp, prosent=$prosent, utbetaling=${beløp()})"
    }

}

class UtregningSammenslåer : SegmentSammenslåer<Beløp, Prosent, Utbetaling> {
    override fun sammenslå(
        periode: Periode,
        venstreSegment: Segment<Beløp>?,
        høyreSegment: Segment<Prosent>?
    ): Segment<Utbetaling> {
//        if (venstreSegment?.verdi == null || høyreSegment?.verdi == null) {
//            return null
//        }
        val beløp = venstreSegment?.verdi ?: Beløp(0)
        val prosent = høyreSegment?.verdi ?: Prosent(0)
        return Segment(periode, Utbetaling(beløp, prosent))
    }

}