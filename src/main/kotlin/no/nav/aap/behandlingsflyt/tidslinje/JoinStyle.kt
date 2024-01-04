package no.nav.aap.behandlingsflyt.tidslinje

fun interface JoinStyle {
    fun accept(harLhs: Boolean, harRhs: Boolean): Boolean

    /**
     * Ene eller andre har verdi.
     */
    object CROSS_JOIN : JoinStyle by JoinStyle(
        { harLhs, harRhs -> harLhs || harRhs }
    )

    /**
     * kun venstre tidsserie.
     */
    object DISJOINT : JoinStyle by JoinStyle(
        { harLhs, harRhs -> harLhs && !harRhs }
    )

    /**
     * kun dersom begge tidsserier har verdi.
     */
    object INNER_JOIN : JoinStyle by JoinStyle(
        { harLhs, harRhs -> harLhs && harRhs }
    )

    /**
     * alltid venstre tidsserie (LHS), høyre (RHS) kun med verdi dersom matcher. Combinator funksjon må hensyn ta
     * nulls for RHS.
     */
    object LEFT_JOIN : JoinStyle by JoinStyle(
        { harLhs, _ -> harLhs }
    )

    /**
     * alltid høyre side (RHS), venstre kun med verdi dersom matcher. Combinator funksjon må hensyn ta nulls for
     * LHS.
     */
    object RIGHT_JOIN : JoinStyle by JoinStyle(
        { _, harRhs -> harRhs }
    )
}
