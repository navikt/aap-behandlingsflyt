package no.nav.aap.behandlingsflyt.underveis.tidslinje

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
    fun kombiner(tidslinje: Tidslinje<T>, sammenslåer: SegmentSammenslåer<T> = PrioriterHøyreSide()): Tidslinje<T> {
        val nySammensetning: NavigableSet<Segment<T>> = TreeSet(segmenter)
        for (segment in tidslinje.segmenter) {
            leggTilPeriode(segment, nySammensetning, sammenslåer)
        }

        return Tidslinje(nySammensetning)
    }

    fun komprimer(): Tidslinje<T> {
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

    private fun leggTilPeriode(
        segment: Segment<T>,
        segments: NavigableSet<Segment<T>>,
        sammenslåer: SegmentSammenslåer<T>
    ) {
        if (segments.any { vp -> vp.periode.overlapper(segment.periode) }) {
            // Overlapper og må justere innholdet i listen
            val skalHåndteres = segments
                .filter { eksisterendeSegment -> eksisterendeSegment.periode.overlapper(segment.periode) }
                .toSet()

            segments.removeAll(skalHåndteres)

            skalHåndteres.forEach { eksisterendeSegment ->
                (eksisterendeSegment.splittEtter(segment) + segment.except(eksisterendeSegment)).forEach {
                    val left = if (eksisterendeSegment.periode.overlapper(it)) {
                        eksisterendeSegment
                    } else {
                        null
                    }
                    val right = if (segment.periode.overlapper(it)) {
                        segment
                    } else {
                        null
                    }
                    segments.add(sammenslåer.sammenslå(it, left, right))
                }
            }
        } else {
            segments.add(segment)
        }
    }

}
