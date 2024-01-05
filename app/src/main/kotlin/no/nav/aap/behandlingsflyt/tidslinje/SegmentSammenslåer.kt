package no.nav.aap.behandlingsflyt.tidslinje

import no.nav.aap.behandlingsflyt.verdityper.Periode

fun interface SegmentSammenslåer<Q, E, V> {

    fun sammenslå(periode: Periode, venstreSegment: Segment<Q>?, høyreSegment: Segment<E>?): Segment<V>?
}