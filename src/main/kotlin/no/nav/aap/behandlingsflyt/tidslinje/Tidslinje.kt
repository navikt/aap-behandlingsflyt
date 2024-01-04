package no.nav.aap.behandlingsflyt.tidslinje

import no.nav.aap.behandlingsflyt.Periode
import java.time.LocalDate
import java.time.Period
import java.util.*
import java.util.function.Function


class Tidslinje<T>(initSegmenter: NavigableSet<Segment<T>> = TreeSet()) : Iterable<Segment<T>> {

    constructor(initSegmenter: List<Segment<T>>) : this(TreeSet(initSegmenter))
    constructor(periode: Periode, verdi: T?) : this(TreeSet(listOf(Segment(periode, verdi))))

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

    fun perioder(): NavigableSet<Periode> {
        return TreeSet(segmenter.map { it.periode })
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
        if (this.segmenter.isEmpty()) {
            val nyeSegmenter = other.segmenter.map { segment ->
                sammenslåer.sammenslå(segment.periode, null, segment)
            }
            return Tidslinje(nyeSegmenter.toCollection(TreeSet()))
        }
        if (other.segmenter.isEmpty()) {
            val nyeSegmenter = this.segmenter.map { segment ->
                sammenslåer.sammenslå(segment.periode, segment, null)
            }
            return Tidslinje(nyeSegmenter.toCollection(TreeSet()))
        }

        val periodeIterator = PeriodeIterator(
            perioder(),
            other.perioder()
        )

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

    fun disjoint(periode: Periode): Tidslinje<T> {
        val intervalTimeline: Tidslinje<Any?> = Tidslinje(listOf(Segment(periode, null)))
        return disjoint(intervalTimeline, StandardSammenslåere.kunVenstre())
    }

    fun <V, R> disjoint(other: Tidslinje<V>, combinator: SegmentSammenslåer<T, V, R>): Tidslinje<R> {
        return kombiner(other, combinator, JoinStyle.DISJOINT)
    }

    fun kryss(periode: Periode): Tidslinje<T> {
        return kombiner(Tidslinje(periode, null), StandardSammenslåere.kunVenstre(), JoinStyle.INNER_JOIN)
    }

    fun kryss(other: Tidslinje<Any?>): Tidslinje<T> {
        return kombiner(other, StandardSammenslåere.kunVenstre(), JoinStyle.INNER_JOIN)
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

    fun <R> mapValue(mapper: Function<T?, R>): Tidslinje<R> {
        val newSegments: NavigableSet<Segment<R>> = TreeSet()
        segmenter.forEach { s ->
            newSegments.add(
                Segment(
                    s.periode,
                    mapper.apply(s.verdi)
                )
            )
        }
        return Tidslinje(newSegments)
    }

    fun splittOppEtter(period: Period): Tidslinje<T> {
        if (segmenter.isEmpty()) {
            return this
        }
        return splittOppEtter(minDato(), maxDato(), period)
    }

    fun splittOppEtter(startDato: LocalDate, period: Period): Tidslinje<T> {
        if (segmenter.isEmpty()) {
            return this
        }
        return splittOppEtter(startDato, maxDato(), period)
    }

    /**
     * Knekker opp segmenterene i henhold til period fom startDato tom sluttDato
     */
    fun splittOppEtter(startDato: LocalDate, sluttDato: LocalDate, period: Period): Tidslinje<T> {
        require(!(LocalDate.MIN == startDato || LocalDate.MAX == sluttDato || sluttDato.isBefore(startDato))) {
            String.format(
                "kan ikke periodisere tidslinjen mellom angitte datoer: [%s, %s]",
                startDato,
                sluttDato
            )
        }

        val segmenter: NavigableSet<Segment<T>> = TreeSet()

        val maxLocalDate: LocalDate = minOf(maxDato(), sluttDato)
        var dt = startDato
        while (!dt.isAfter(maxLocalDate)) {
            val nextDt = dt.plus(period)

            val nesteSegmenter: NavigableSet<Segment<T>> = kryss(Periode(dt, nextDt.minusDays(1))).segmenter
            segmenter.addAll(nesteSegmenter)
            dt = nextDt
        }
        return Tidslinje(segmenter)
    }

    fun <R> splittOppOgMapOmEtter(
        period: Period,
        mapper: Function<NavigableSet<Segment<T>>, NavigableSet<Segment<R>>>
    ): Tidslinje<R> {
        if (segmenter.isEmpty()) {
            return Tidslinje<R>()
        }
        return splittOppOgMapOmEtter(minDato(), maxDato(), period, mapper)
    }

    /**
     * Knekker opp segmenterene i henhold til period fom startDato tom sluttDato
     */
    fun <R> splittOppOgMapOmEtter(
        startDato: LocalDate,
        sluttDato: LocalDate,
        period: Period,
        mapper: Function<NavigableSet<Segment<T>>, NavigableSet<Segment<R>>>
    ): Tidslinje<R> {
        require(!(LocalDate.MIN == startDato || LocalDate.MAX == sluttDato || sluttDato.isBefore(startDato))) {
            String.format(
                "kan ikke periodisere tidslinjen mellom angitte datoer: [%s, %s]",
                startDato,
                sluttDato
            )
        }

        val segmenter: NavigableSet<Segment<R>> = TreeSet()

        val maxLocalDate: LocalDate = minOf(maxDato(), sluttDato)
        var dt = startDato
        while (!dt.isAfter(maxLocalDate)) {
            val nextDt = dt.plus(period)

            val nesteSegmenter: NavigableSet<Segment<T>> = kryss(Periode(dt, nextDt.minusDays(1))).segmenter
            segmenter.addAll(mapper.apply(nesteSegmenter))
            dt = nextDt
        }
        return Tidslinje(segmenter)
    }

    /**
     * Henter segmentet som inneholder datoen
     */
    fun segment(dato: LocalDate): Segment<T>? {
        return segmenter.firstOrNull { segment -> segment.inneholder(dato) }
    }

    override fun iterator(): Iterator<Segment<T>> {
        return segmenter.iterator()
    }

    fun minDato(): LocalDate {
        check(!segmenter.isEmpty()) {
            "Timeline is empty" //$NON-NLS-1$
        }
        return segmenter.first().fom()
    }

    fun maxDato(): LocalDate {
        check(!segmenter.isEmpty()) {
            "Timeline is empty" //$NON-NLS-1$
        }
        return segmenter.last().tom()
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

    override fun toString(): String {
        return "Tidslinje(segmenter=$segmenter)"
    }
}
