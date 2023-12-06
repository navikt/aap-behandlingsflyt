package no.nav.aap.behandlingsflyt.underveis.tidslinje

import no.nav.aap.behandlingsflyt.Periode

class PrioriterHøyreSide<T> : SegmentSammenslåer<T> {
    override fun sammenslå(periode: Periode, venstreSegment: Segment<T>?, høyreSegment: Segment<T>?): Segment<T> {
        if (høyreSegment == null) {
            return Segment(periode, venstreSegment?.verdi)
        }
        return Segment(periode, høyreSegment.verdi)
    }
}

class PrioriteVenstreSide<T> : SegmentSammenslåer<T> {
    override fun sammenslå(periode: Periode, venstreSegment: Segment<T>?, høyreSegment: Segment<T>?): Segment<T> {
        if (venstreSegment == null) {
            return Segment(periode, høyreSegment?.verdi)
        }
        return Segment(periode, venstreSegment.verdi)
    }
}

class KunHøyre<T> : SegmentSammenslåer<T> {
    override fun sammenslå(periode: Periode, venstreSegment: Segment<T>?, høyreSegment: Segment<T>?): Segment<T> {
        return Segment(periode, høyreSegment?.verdi)
    }
}

class KunVenstre<T> : SegmentSammenslåer<T> {
    override fun sammenslå(periode: Periode, venstreSegment: Segment<T>?, høyreSegment: Segment<T>?): Segment<T> {
        return Segment(periode, venstreSegment?.verdi)
    }
}