package no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom

import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje

data class TidslinjeQuintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)

fun <A, B, C, D, E> Tidslinje.Companion.zip5(
    aTidslinje: Tidslinje<A>,
    bTidslinje: Tidslinje<B>,
    cTidslinje: Tidslinje<C>,
    dTidslinje: Tidslinje<D>,
    eTidslinje: Tidslinje<E>
): Tidslinje<TidslinjeQuintuple<A?, B?, C?, D?, E?>> {
    val ab = aTidslinje.kombiner(bTidslinje, JoinStyle.OUTER_JOIN { periode, aSeg, bSeg ->
        Segment(periode, Pair(aSeg?.verdi, bSeg?.verdi))
    })

    val abc = ab.kombiner(cTidslinje, JoinStyle.OUTER_JOIN { periode, abSeg, cSeg ->
        val aVal = abSeg?.verdi?.first
        val bVal = abSeg?.verdi?.second
        Segment(periode, Triple(aVal, bVal, cSeg?.verdi))
    })

    val abcd = abc.kombiner(dTidslinje, JoinStyle.OUTER_JOIN { periode, abcSeg, dSeg ->
        Segment(periode, Pair(abcSeg?.verdi, dSeg?.verdi))
    })

    return abcd.kombiner(eTidslinje, JoinStyle.OUTER_JOIN { periode, abcdSeg, eSeg ->
        val t = abcdSeg?.verdi?.first
        val dVal = abcdSeg?.verdi?.second
        Segment(
            periode,
            TidslinjeQuintuple(
                t?.first,
                t?.second,
                t?.third,
                dVal,
                eSeg?.verdi
            )
        )
    })
}