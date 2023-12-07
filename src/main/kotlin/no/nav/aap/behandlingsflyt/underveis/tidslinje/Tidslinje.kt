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
    fun <E, V> kombiner(
        other: Tidslinje<E>,
        sammenslåer: SegmentSammenslåer<T, E, V>
    ): Tidslinje<V> {

        val periodeIterator: PeriodeIterator<T, E> = PeriodeIterator(this.segmenter, other.segmenter)
        if (!periodeIterator.hasNext()) {
            return Tidslinje(emptyList()) //begge input-tidslinjer var tomme
        }

        val nySammensetning: NavigableSet<Segment<V>> = TreeSet()
        while (periodeIterator.hasNext()) {
            val periode = periodeIterator.next()

            val left =
                this.segmenter.firstOrNull { segment -> segment.periode.overlapper(periode) }?.tilpassetPeriode(periode)
            val right = other.segmenter.firstOrNull { segment -> segment.periode.overlapper(periode) }
                ?.tilpassetPeriode(periode)

            val element = sammenslåer.sammenslå(periode, left, right)
            if (element != null) {
                nySammensetning.add(element)
            }
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
}
