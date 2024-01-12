package no.nav.aap.verdityper

import java.time.LocalDate
import java.time.Period
import java.util.*

class Periode(val fom: LocalDate, val tom: LocalDate) : Comparable<Periode> {

    init {
        require(fom <= tom) { "tom($tom) er før fom($fom)" }
    }

    fun overlapper(other: Periode): Boolean {
        return this.fom <= other.tom && other.fom <= this.tom
    }

    fun inneholder(dato: LocalDate): Boolean {
        return dato in fom..tom
    }

    fun antallDager(): Int {
        return Period.between(fom, tom.plusDays(1)).days
    }

    override fun compareTo(other: Periode): Int {
        val compareFom = fom.compareTo(other.fom)

        if (compareFom != 0) {
            return compareFom
        }

        return tom.compareTo(other.tom)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Periode

        if (fom != other.fom) return false
        return tom == other.tom
    }

    override fun hashCode(): Int {
        var result = fom.hashCode()
        result = 31 * result + tom.hashCode()
        return result
    }

    override fun toString(): String {
        return "Periode(fom=$fom, tom=$tom)"
    }

    fun jsonValue(): String {
        return "$fom/$tom"
    }

    fun overlapp(other: Periode): Periode? {
        return if (!this.overlapper(other)) {
            null
        } else if (this == other) {
            this
        } else {
            Periode(maxOf(fom, other.fom), minOf(tom, other.tom))
        }
    }

    fun minus(other: Periode): NavigableSet<Periode> {
        if (!this.overlapper(other)) {
            return TreeSet(listOf(this))
        }
        val resultat: NavigableSet<Periode> = TreeSet()
        if (fom < other.fom) {
            resultat.add(Periode(fom, minOf(tom, other.fom.minusDays(1))))
        }
        if (tom > other.tom) {
            resultat.add(Periode(maxOf(fom, other.tom.plusDays(1)), tom))
        }
        return resultat
    }

    fun utvid(other: Periode): Periode {
        return Periode(minOf(this.fom, other.fom), maxOf(this.tom, other.tom))
    }

    fun inntil(other: Periode): Boolean {
        return this.tom == other.fom.minusDays(1) || other.tom == this.fom.minusDays(1)
    }
}
