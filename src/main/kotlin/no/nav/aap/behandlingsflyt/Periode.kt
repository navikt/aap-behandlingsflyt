package no.nav.aap.behandlingsflyt

import java.time.LocalDate

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
}
