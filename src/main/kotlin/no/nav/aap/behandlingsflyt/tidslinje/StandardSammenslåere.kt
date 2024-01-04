package no.nav.aap.behandlingsflyt.tidslinje

import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.Beløp
import java.math.BigDecimal

object StandardSammenslåere {
    fun summerer(): SegmentSammenslåer<Beløp, Beløp, Beløp> {
        return SegmentSammenslåer { periode, venstreSegment, høyreSegment ->
            val høyreVerdi = høyreSegment?.verdi ?: Beløp(BigDecimal.ZERO)
            val venstreVerdi = venstreSegment?.verdi ?: Beløp(BigDecimal.ZERO)

            Segment(periode, høyreVerdi.pluss(venstreVerdi))
        }
    }

    fun <T> prioriterHøyreSide(): SegmentSammenslåer<T, T, T> {
        return SegmentSammenslåer { periode, venstreSegment, høyreSegment ->
            if (høyreSegment == null) {
                Segment(periode, venstreSegment?.verdi)
            } else {
                Segment(periode, høyreSegment.verdi)
            }
        }
    }

    fun <T> prioriterVenstreSide(): SegmentSammenslåer<T, T, T> {
        return SegmentSammenslåer { periode, venstreSegment, høyreSegment ->
            if (venstreSegment == null) {
                Segment(periode, høyreSegment?.verdi)
            } else {
                Segment(periode, venstreSegment.verdi)
            }
        }
    }

    fun <T> kunVenstre(): SegmentSammenslåer<T, Any?, T> {
        return SegmentSammenslåer { periode, venstreSegment, _ ->
            val verdi = venstreSegment?.verdi
            if (verdi == null) {
                null
            } else {
                Segment(periode, verdi)
            }
        }
    }

    fun <T> kunHøyre(): SegmentSammenslåer<Any?, T, T> {
        return SegmentSammenslåer { periode, _, høyreSegment ->
            val verdi = høyreSegment?.verdi
            if (verdi == null) {
                null
            } else {
                Segment(periode, verdi)
            }
        }
    }
}