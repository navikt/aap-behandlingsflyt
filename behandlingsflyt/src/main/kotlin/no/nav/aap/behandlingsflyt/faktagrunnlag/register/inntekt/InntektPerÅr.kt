package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.komponenter.verdityper.Beløp
import java.time.Year

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