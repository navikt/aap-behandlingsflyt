package no.nav.aap.behandlingsflyt.underveis.tidslinje

import no.nav.aap.behandlingsflyt.Periode

interface SegmentSammenslåer<T> {

    fun sammenslå(periode: Periode, venstreSegment: Segment<T>?, høyreSegment: Segment<T>?): Segment<T>
}