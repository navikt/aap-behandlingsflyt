package no.nav.aap.beregning

import java.time.Year
import no.nav.aap.komponenter.verdityper.Beløp

data class InntektPerÅr(
    val år: Year,
    val beløp: Beløp,
) : Comparable<InntektPerÅr> {
    constructor(år: Int, beløp: Beløp) : this(Year.of(år), beløp)

    fun gUnit(): Grunnbeløp.BenyttetGjennomsnittsbeløp =
        Grunnbeløp.finnGUnit(år, beløp)

    override fun compareTo(other: InntektPerÅr): Int =
        this.år.compareTo(other.år)
}