package no.nav.aap.behandlingsflyt.underveis.tidslinje

import no.nav.aap.behandlingsflyt.Periode
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
        assertThat(tidslinje.compress().segmenter()).containsExactly(
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

        val mergetTidslinje = tidslinje.mergeMed(tidslinje1)

        assertThat(mergetTidslinje.segmenter()).containsExactly(secondSegment, firstSegment)
    }

    @Test
    fun `skal slå sammen to tidslinjer med overlapp`() {
        val firstSegment = Segment(Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(10)), Beløp(100))
        val expectedFirstSegment = Segment(Periode(LocalDate.now(), LocalDate.now().plusDays(10)), Beløp(100))
        val secondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)), Beløp(200))
        val tidslinje = Tidslinje(listOf(firstSegment))
        val tidslinje1 = Tidslinje(listOf(secondSegment))

        val mergetTidslinje = tidslinje.mergeMed(tidslinje1).compress()

        assertThat(mergetTidslinje.segmenter()).containsExactly(secondSegment, expectedFirstSegment)
    }

    @Test
    fun `skal kunne styre prioritet mellom tidslinjer`() {
        val firstSegment = Segment(Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(10)), Beløp(100))
        val secondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)), Beløp(200))
        val expectedSecondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(3)), Beløp(200))
        val tidslinje = Tidslinje(listOf(firstSegment))
        val tidslinje1 = Tidslinje(listOf(secondSegment))

        val mergetTidslinje = tidslinje.mergeMed(tidslinje1, PrioriteVenstreSide()).compress()

        assertThat(mergetTidslinje.segmenter()).containsExactly(expectedSecondSegment, firstSegment)
    }


}