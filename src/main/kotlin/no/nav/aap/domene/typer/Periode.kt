package no.nav.aap.domene.typer

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.time.LocalDate

class Periode(private val fom: LocalDate, private val tom: LocalDate) {

    @JsonCreator
    constructor(periodeString: String)
            : this(LocalDate.parse(periodeString.split("/")[0]), LocalDate.parse(periodeString.split("/")[1]))

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
        return !this.tom.isBefore(periode.fraOgMed()) && !this.fom.isAfter(periode.tilOgMed())
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

    @JsonValue
    fun jsonValue(): String {
        return "$fom/$tom"
    }
}
