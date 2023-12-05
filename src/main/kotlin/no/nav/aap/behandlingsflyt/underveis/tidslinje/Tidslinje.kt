package no.nav.aap.behandlingsflyt.underveis.tidslinje

import no.nav.aap.behandlingsflyt.Periode
import java.util.*

class Tidslinje<T>(initSegmenter: NavigableSet<Segment<T>>) {

    constructor(initSegmenter: List<Segment<T>>) : this(TreeSet(initSegmenter))

    private val segmenter: NavigableSet<Segment<T>> = TreeSet()

    init {
        segmenter.addAll(initSegmenter)
        // Sjekk etter overlapp
        validerIkkeOverlapp()
    }

    private fun validerIkkeOverlapp() {
        var last: Segment<T>? = null
        for (seg in segmenter) {
            if (last != null) {
                require(!seg.overlapper(last)) { String.format("Overlapp %s - %s", last, seg) }
            }
            last = seg
        }
    }

    fun segmenter(): List<Segment<T>> {
        return segmenter.toList()
    }

    /**
     * Merge av to tidslinjer, prioriterer verdier fra den som merges over den som det kalles på
     * oppretter en tredje slik at orginale verdier bevares
     */
    fun mergeMed(tidslinje: Tidslinje<T>): Tidslinje<T> {
        val nySammensetning: NavigableSet<Segment<T>> = TreeSet(segmenter)
        for (segment in tidslinje.segmenter) {
            leggTilPeriode(segment, nySammensetning)
        }

        return Tidslinje(nySammensetning)
    }

    fun compress(): Tidslinje<T> {
        val compressedSegmenter: NavigableSet<Segment<T>> = TreeSet()
        segmenter.forEach { segment ->
            if (compressedSegmenter.isEmpty()) {
                compressedSegmenter.add(segment)
            } else {
                val nærliggendeSegment =
                    compressedSegmenter.firstOrNull { it.inntil(segment) && it.verdi == segment.verdi }
                if (nærliggendeSegment != null) {
                    val forlengetKopi = nærliggendeSegment.forlengetKopi(segment.periode)
                    compressedSegmenter.remove(nærliggendeSegment)
                    compressedSegmenter.add(forlengetKopi)
                } else {
                    compressedSegmenter.add(segment)
                }
            }
        }
        return Tidslinje(compressedSegmenter)
    }

    private fun leggTilPeriode(segment: Segment<T>, segments: NavigableSet<Segment<T>>) {
        if (segments.any { vp -> vp.periode.overlapper(segment.periode) }) {
            // Overlapper og må justere innholdet i listen
            val justertePerioder = segments
                .filter { vp -> vp.periode.overlapper(segment.periode) }
                .filter { vp ->
                    vp.periode.tom > segment.periode.tom || vp.periode.fom < segment.periode.fom
                }
                .map { vp ->
                    Segment(justerPeriode(vp.periode, segment.periode), vp.verdi)
                }

            segments.removeIf { vp -> vp.periode.overlapper(segment.periode) }
            segments.addAll(justertePerioder)
            segments.add(segment)
        } else {
            segments.add(segment)
        }
    }

    private fun justerPeriode(leftPeriode: Periode, rightPeriode: Periode): Periode {
        if (!leftPeriode.overlapper(rightPeriode)) {
            return leftPeriode
        }
        val fom = if (rightPeriode.fom.isBefore(leftPeriode.fom)) {
            rightPeriode.tom.plusDays(1)
        } else {
            leftPeriode.fom
        }
        val tom = if (rightPeriode.tom.isAfter(leftPeriode.tom)) {
            rightPeriode.fom.minusDays(1)
        } else {
            leftPeriode.tom
        }
        return Periode(fom, tom)
    }
}
