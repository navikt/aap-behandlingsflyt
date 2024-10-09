package no.nav.aap.tidslinje

import no.nav.aap.verdityper.Beløp
import java.math.BigDecimal

object StandardSammenslåere {
    fun summerer(): JoinStyle.OUTER_JOIN<Beløp, Beløp, Beløp> {
        return JoinStyle.OUTER_JOIN { periode, venstreSegment, høyreSegment ->
            val høyreVerdi = høyreSegment?.verdi ?: Beløp(BigDecimal.ZERO)
            val venstreVerdi = venstreSegment?.verdi ?: Beløp(BigDecimal.ZERO)

            Segment(periode, høyreVerdi.pluss(venstreVerdi))
        }
    }

    fun <T : Any> prioriterHøyreSide(): JoinStyle.INNER_JOIN<T, T, T> {
        return JoinStyle.INNER_JOIN { periode, _, høyreSegment ->
            Segment(periode, høyreSegment.verdi)
        }
    }

    fun <T> prioriterHøyreSideCrossJoin(): JoinStyle.OUTER_JOIN<T, T, T> {
        return JoinStyle.OUTER_JOIN { periode, venstre, høyre ->
            if (høyre != null) return@OUTER_JOIN Segment(periode, høyre.verdi)
            if (venstre == null) return@OUTER_JOIN null
            Segment(periode, venstre.verdi)
        }
    }

    fun <T> prioriterVenstreSideCrossJoin(): JoinStyle.OUTER_JOIN<T, T, T> {
        return JoinStyle.OUTER_JOIN { periode, venstreSegment, høyreSegment ->
            if (venstreSegment != null) return@OUTER_JOIN Segment(periode, venstreSegment.verdi)
            if (høyreSegment == null) return@OUTER_JOIN null
            Segment(periode, høyreSegment.verdi)
        }
    }

    fun <T, S> kunVenstre(): JoinStyle.INNER_JOIN<T, S, T> {
        return JoinStyle.INNER_JOIN { periode, venstreSegment, _ ->
            Segment(periode, venstreSegment.verdi)
        }
    }

    fun <T, S> kunHøyreLeftJoin(): JoinStyle.LEFT_JOIN<S, T, T> {
        return JoinStyle.LEFT_JOIN { periode, _, høyreSegment ->
            if (høyreSegment == null) return@LEFT_JOIN null
            Segment(periode, høyreSegment.verdi)
        }
    }

    fun <T, S> kunHøyreRightJoin(): JoinStyle.RIGHT_JOIN<S, T, T> {
        return JoinStyle.RIGHT_JOIN { periode, _, høyreSegment ->
            Segment(periode, høyreSegment.verdi)
        }
    }
}