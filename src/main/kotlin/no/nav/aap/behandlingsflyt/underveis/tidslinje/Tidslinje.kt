package no.nav.aap.behandlingsflyt.underveis.tidslinje

import java.time.LocalDate
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

    fun segmenter(): SortedSet<Segment<T>> {
        return segmenter.toSortedSet()
    }

    /**
     * Merge av to tidslinjer, prioriterer verdier fra den som merges over den som det kalles på
     * oppretter en tredje slik at orginale verdier bevares
     */
    fun <E, V> kombiner(
        other: Tidslinje<E>,
        sammenslåer: SegmentSammenslåer<T, E, V>,
        joinStyle: JoinStyle = JoinStyle.CROSS_JOIN
    ): Tidslinje<V> {

        val periodeIterator: PeriodeIterator<T, E> = PeriodeIterator(this.segmenter, other.segmenter)
        if (!periodeIterator.hasNext()) {
            return Tidslinje(emptyList()) //begge input-tidslinjer var tomme
        }

        val nySammensetning: NavigableSet<Segment<V>> = TreeSet()
        while (periodeIterator.hasNext()) {
            val periode = periodeIterator.next()

            val left = this.segmenter.firstOrNull { segment -> segment.periode.overlapper(periode) }
                ?.tilpassetPeriode(periode)
            val right = other.segmenter.firstOrNull { segment -> segment.periode.overlapper(periode) }
                ?.tilpassetPeriode(periode)

            if (joinStyle.accept(left != null, right != null)) {
                val element = sammenslåer.sammenslå(periode, left, right)
                if (element != null) {
                    nySammensetning.add(element)
                }
            }
        }

        return Tidslinje(nySammensetning)
    }

    fun <V> kryss(other: Tidslinje<V>): Tidslinje<T> {
        return kombiner(other, KunVenstre(), JoinStyle.INNER_JOIN)
    }

    /**
     * Komprimerer tidslinjen
     * - Slår sammen segmetner hvor verdien er identisk (benytter equals for sjekk)
     */
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

    /**
     * Henter segmentet som inneholder datoen
     */
    fun segment(dato: LocalDate): Segment<T>? {
        return segmenter.firstOrNull { segment -> segment.inneholder(dato) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tidslinje<*>

        // Benytter hashset for å slippe rør med compareTo osv..
        return HashSet(segmenter) == HashSet(other.segmenter)
    }

    override fun hashCode(): Int {
        return segmenter.hashCode()
    }
}
