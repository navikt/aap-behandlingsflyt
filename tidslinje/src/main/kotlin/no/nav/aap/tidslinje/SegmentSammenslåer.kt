package no.nav.aap.tidslinje

import no.nav.aap.verdityper.Periode

fun interface SegmentSammenslåer<Q, E, V> {

    fun sammenslå(periode: Periode, venstreSegment: Segment<Q>?, høyreSegment: Segment<E>?): Segment<V>?
}