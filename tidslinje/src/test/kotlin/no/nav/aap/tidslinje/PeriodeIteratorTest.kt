package no.nav.aap.tidslinje

import no.nav.aap.verdityper.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class PeriodeIteratorTest {

    @Test
    fun `skal lage iterator for alle unike perioder`() {
        val fullPeriode = Periode(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10))
        val delPeriode1 = Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(7))
        val delPeriode2 = Periode(LocalDate.now().minusDays(5), LocalDate.now())
        val delPeriode3 = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(10))

        val segmenter1 = TreeSet(listOf(fullPeriode))
        val segmenter2 = TreeSet(
            TreeSet(listOf(
                delPeriode1,
                delPeriode2,
                delPeriode3
            ))
        )

        val iterator = PeriodeIterator(segmenter1, segmenter2)

        val setMedDatoer = TreeSet<Periode>(emptyList())

        while (iterator.hasNext()) {
            setMedDatoer.add(iterator.next())
        }

        assertThat(setMedDatoer).hasSize(4)

        val iterator1 = PeriodeIterator(segmenter2, segmenter1)
        val setMedDatoer1 = TreeSet<Periode>(emptyList())

        while (iterator1.hasNext()) {
            setMedDatoer1.add(iterator1.next())
        }

        assertThat(setMedDatoer1).hasSize(4)
        assertThat(setMedDatoer).isEqualTo(setMedDatoer1)
        assertThat(setMedDatoer.first.fom).isEqualTo(setMedDatoer1.first.fom)
        assertThat(setMedDatoer.last.tom).isEqualTo(setMedDatoer1.last.tom)
    }

    @Test
    fun `skal lage iterator for alle unike perioder 2`() {
        val fullPeriode = Periode(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10))
        val delPeriode1 = Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(7))
        val delPeriode2 = Periode(LocalDate.now().minusDays(5), LocalDate.now())
        val delPeriode3 = Periode(LocalDate.now().plusDays(12), LocalDate.now().plusDays(20))

        val segmenter1 = TreeSet(listOf(fullPeriode))
        val segmenter2 = TreeSet(
            TreeSet(listOf(
                delPeriode1,
                delPeriode2,
                delPeriode3
            ))
        )

        val iterator = PeriodeIterator(segmenter1, segmenter2)

        val setMedDatoer = TreeSet<Periode>(emptyList())

        while (iterator.hasNext()) {
            setMedDatoer.add(iterator.next())
        }

        assertThat(setMedDatoer).hasSize(5)

        val iterator1 = PeriodeIterator(segmenter2, segmenter1)
        val setMedDatoer1 = TreeSet<Periode>(emptyList())

        while (iterator1.hasNext()) {
            setMedDatoer1.add(iterator1.next())
        }

        assertThat(setMedDatoer1).hasSize(5)
        assertThat(setMedDatoer).isEqualTo(setMedDatoer1)
        assertThat(setMedDatoer.first.fom).isEqualTo(setMedDatoer1.first.fom)
        assertThat(setMedDatoer.last.tom).isEqualTo(setMedDatoer1.last.tom)
    }
}