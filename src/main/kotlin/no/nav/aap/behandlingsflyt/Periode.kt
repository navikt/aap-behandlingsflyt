package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.tidslinje.max
import no.nav.aap.behandlingsflyt.tidslinje.min
import java.time.LocalDate
import java.time.Period
import java.util.*

class Periode(val fom: LocalDate, val tom: LocalDate) : Comparable<Periode> {

    init {
        valider()
    }

    private fun valider() {
        if (tom.isBefore(fom) && !tom.isEqual(fom)) {
            throw IllegalArgumentException("tom($tom) er før fom($fom)")
        }
    }

    fun overlapper(periode: Periode): Boolean {
        return !this.tom.isBefore(periode.fom) && !this.fom.isAfter(periode.tom)
    }

    fun antallDager(): Int {
        return Period.between(fom, tom.plusDays(1)).days
    }

    override fun compareTo(other: Periode): Int {
        val compareFom = fom.compareTo(other.fom);

        if (compareFom != 0) {
            return compareFom;
        }

        return tom.compareTo(other.tom);
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

    fun overlapp(periode: Periode): Periode? {
        return if (!this.overlapper(periode)) {
            null
        } else if (this == periode) {
            this
        } else {
            Periode(max(fom, periode.fom), min(tom, periode.tom))
        }
    }

    fun minus(annen: Periode): NavigableSet<Periode> {
        if (!this.overlapper(annen)) {
            return TreeSet(listOf(this))
        }
        val resultat: NavigableSet<Periode> = TreeSet()
        if (fom.isBefore(annen.fom)) {
            resultat.add(Periode(fom, min(tom, annen.fom.minusDays(1))))
        }
        if (tom.isAfter(annen.tom)) {
            resultat.add(Periode(max(fom, annen.tom.plusDays(1)), tom))
        }
        return resultat
    }

    fun inneholder(dato: LocalDate): Boolean {
        return overlapp(Periode(dato, dato)) != null
    }
}
