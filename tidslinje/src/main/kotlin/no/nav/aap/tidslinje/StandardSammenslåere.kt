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

    /**
     * ```
     *             venstre høyre  priorterHøyreSide
     * 2020-01-01  +---+
     *             | x |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |  | 1          |
     * 2020-01-03  +---+   |   |  +------------+
     *                     |   |
     * 2020-01-04          +---+
     *
     * 2020-01-05  +---+
     *             | y |
     * 2020-01-06  +---+
     * ```
     */
    fun <T : Any> prioriterHøyreSide(): JoinStyle.INNER_JOIN<T, T, T> {
        return JoinStyle.INNER_JOIN { periode, _, høyreSegment ->
            Segment(periode, høyreSegment.verdi)
        }
    }

    /**
     * ```
     *             venstre høyre  priorterHøyreSideCrossJoin
     * 2020-01-01  +---+          +------------+
     *             | x |          | x          |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |  | 1          |
     * 2020-01-03  +---+   |   |  +------------+
     *                     |   |  | 1          |
     * 2020-01-04          +---+  +------------+
     *
     * 2020-01-05  +---+          +------------+
     *             | y |          | y          |
     * 2020-01-06  +---+          +------------+
     * ```
     */
    fun <T> prioriterHøyreSideCrossJoin(): JoinStyle.OUTER_JOIN<T, T, T> {
        return JoinStyle.OUTER_JOIN { periode, venstre, høyre ->
            if (høyre != null) return@OUTER_JOIN Segment(periode, høyre.verdi)
            if (venstre == null) return@OUTER_JOIN null
            Segment(periode, venstre.verdi)
        }
    }

    /**
     * ```
     *             venstre høyre  priorterVenstreSideCrossJoin
     * 2020-01-01  +---+          +------------+
     *             | x |          | x          |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |  | x          |
     * 2020-01-03  +---+   |   |  +------------+
     *                     |   |  | 1          |
     * 2020-01-04          +---+  +------------+
     *
     * 2020-01-05  +---+          +------------+
     *             | y |          | y          |
     * 2020-01-06  +---+          +------------+
     * ```
     */
    fun <T> prioriterVenstreSideCrossJoin(): JoinStyle.OUTER_JOIN<T, T, T> {
        return JoinStyle.OUTER_JOIN { periode, venstreSegment, høyreSegment ->
            if (venstreSegment != null) return@OUTER_JOIN Segment(periode, venstreSegment.verdi)
            if (høyreSegment == null) return@OUTER_JOIN null
            Segment(periode, høyreSegment.verdi)
        }
    }

    /**
     * ```
     *             venstre høyre  kunVenstre
     * 2020-01-01  +---+
     *             | x |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |  | x          |
     * 2020-01-03  +---+   |   |  +------------+
     *                     |   |
     * 2020-01-04          +---+
     *
     * 2020-01-05  +---+
     *             | y |
     * 2020-01-06  +---+
     * ```
     */
    fun <T, S> kunVenstre(): JoinStyle.INNER_JOIN<T, S, T> {
        return JoinStyle.INNER_JOIN { periode, venstreSegment, _ ->
            Segment(periode, venstreSegment.verdi)
        }
    }

    /**
     * ```
     *             venstre høyre  kunHøyreLeftJoin
     * 2020-01-01  +---+
     *             | x |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |  | 1          |
     * 2020-01-03  +---+   |   |  +------------+
     *                     |   |
     * 2020-01-04          +---+
     *
     * 2020-01-05  +---+
     *             | y |
     * 2020-01-06  +---+
     * ```
     */
    fun <T, S> kunHøyreLeftJoin(): JoinStyle.LEFT_JOIN<S, T, T> {
        return JoinStyle.LEFT_JOIN { periode, _, høyreSegment ->
            if (høyreSegment == null) return@LEFT_JOIN null
            Segment(periode, høyreSegment.verdi)
        }
    }

    /**
     * ```
     *             venstre høyre  kunHøyreRightJoin
     * 2020-01-01  +---+
     *             | x |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |  | 1          |
     * 2020-01-03  +---+   |   |  +------------+
     *                     |   |  | 1          |
     * 2020-01-04          +---+  +------------+
     *
     * 2020-01-05  +---+
     *             | y |
     * 2020-01-06  +---+
     * ```
     */
    fun <T, S> kunHøyreRightJoin(): JoinStyle.RIGHT_JOIN<S, T, T> {
        return JoinStyle.RIGHT_JOIN { periode, _, høyreSegment ->
            Segment(periode, høyreSegment.verdi)
        }
    }
}