package no.nav.aap.domene.typer

import java.time.LocalDate

class Periode(private val fom: LocalDate, private val tom: LocalDate) {
    init {
        valider()
    }

    private fun valider() {
        if (tom.isBefore(fom) && !tom.isEqual(fom)) {
            throw IllegalArgumentException("tom($tom) er før fom($fom)")
        }
    }

    fun fraOgMed(): LocalDate {
        return fom
    }

    fun tilOgMed(): LocalDate {
        return tom
    }

    fun overlapper(periode: Periode): Boolean {
        return this == periode || this.fom.compareTo(periode.tom) < 0 && periode.fom.compareTo(this.tom) < 0
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


}
