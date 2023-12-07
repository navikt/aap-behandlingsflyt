package no.nav.aap.behandlingsflyt.underveis.tidslinje

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.beregning.Prosent
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.Beløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class PeriodeIteratorTest {

    @Test
    fun `skal lage iterator for alle unike perioder`() {
        val fullPeriode = Periode(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10))
        val delPeriode1 = Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(6))
        val delPeriode2 = Periode(LocalDate.now().minusDays(5), LocalDate.now())
        val delPeriode3 = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(10))

        val beløp = Beløp(756)
        val firstSegment = Segment(fullPeriode, beløp)

        val segmenter1 = TreeSet(listOf(firstSegment))
        val segmenter2 = TreeSet(
            listOf(
                Segment(delPeriode1, Prosent(10)),
                Segment(delPeriode2, Prosent(50)),
                Segment(delPeriode3, Prosent(78))
            )
        )

        val iterator = PeriodeIterator(segmenter1, segmenter2)

        val setMedDatoer = TreeSet<Periode>(emptyList())

        while (iterator.hasNext()) {
            setMedDatoer.add(iterator.next())
        }

        assertThat(setMedDatoer).hasSize(3)
    }
}