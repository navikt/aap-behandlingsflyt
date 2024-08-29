package no.nav.aap.tidslinje

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate


class Segment<T>(val periode: Periode, val verdi: T) : Comparable<Segment<T>> {
    fun overlapper(segment: Segment<*>): Boolean {
        return periode.overlapper(segment.periode)
    }

    internal fun forlengetKopi(periode: Periode): Segment<T> {
        val utvidetPeriode = this.periode.utvid(periode)
        return Segment(utvidetPeriode, verdi)
    }

    fun inntil(other: Segment<T>): Boolean {
        return this.periode.inntil(other.periode)
    }

    override fun compareTo(other: Segment<T>): Int {
        return this.periode.compareTo(other.periode)
    }

    override fun toString(): String {
        return "Segment(periode=$periode, verdi=$verdi)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Segment<*>

        if (periode != other.periode) return false
        if (verdi != other.verdi) return false

        return true
    }

    override fun hashCode(): Int {
        var result = periode.hashCode()
        result = 31 * result + (verdi?.hashCode() ?: 0)
        return result
    }

    fun tilpassetPeriode(periode: Periode): Segment<T> {
        return Segment(periode, verdi)
    }

    fun inneholder(dato: LocalDate): Boolean {
        return periode.inneholder(dato)
    }

    fun fom(): LocalDate {
        return periode.fom
    }

    fun tom(): LocalDate {
        return periode.tom
    }
}
