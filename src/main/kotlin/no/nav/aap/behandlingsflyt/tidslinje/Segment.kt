package no.nav.aap.behandlingsflyt.tidslinje

import no.nav.aap.behandlingsflyt.Periode
import java.time.LocalDate


class Segment<T>(val periode: Periode, val verdi: T?) : Comparable<Segment<T>> {
    fun overlapper(segment: Segment<*>): Boolean {
        return periode.overlapper(segment.periode)
    }

    internal fun forlengetKopi(periode: Periode): Segment<T> {
        val fom = firstOf(this.periode.fom, periode.fom)
        val tom = lastOf(this.periode.tom, periode.tom)

        return Segment(Periode(fom, tom), verdi)
    }

    private fun lastOf(tom: LocalDate, tom1: LocalDate): LocalDate {
        if (tom < tom1) {
            return tom1
        }
        return tom
    }

    private fun firstOf(fom: LocalDate, fom1: LocalDate): LocalDate {
        if (fom < fom1) {
            return fom
        }
        return fom1
    }

    fun inntil(other: Segment<T>): Boolean {
        return periode.tom == other.periode.fom.minusDays(1) || other.periode.tom == periode.fom.minusDays(1)
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

internal fun min(dato: LocalDate, dato1: LocalDate): LocalDate {
    if (dato.isBefore(dato1)) {
        return dato
    }
    return dato1
}

internal fun max(dato: LocalDate, dato1: LocalDate): LocalDate {
    if (dato.isAfter(dato1)) {
        return dato
    }
    return dato1
}