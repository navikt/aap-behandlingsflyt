package no.nav.aap.tidslinje

import no.nav.aap.komponenter.type.Periode

fun interface SegmentSammenslåer<Q, E, V, RETUR:Segment<V>?> {

    fun sammenslå(periode: Periode, venstreSegment: Q, høyreSegment: E): RETUR?
}