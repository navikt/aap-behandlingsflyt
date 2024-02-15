package no.nav.aap.verdityper

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Faktor av antall G for representasjon av størrelsen på det maksnimalet grunnlaget
 */
class GUnit(verdi: BigDecimal) : Comparable<GUnit> {
    private val verdi = verdi.setScale(SCALE, RoundingMode.HALF_UP)

    constructor(intVerdi: Int) : this(BigDecimal(intVerdi))
    constructor(stringVerdi: String) : this(BigDecimal(stringVerdi))

    companion object {
        const val SCALE = 10

        fun gjennomsnittlig(gUnits: List<GUnit>): GUnit {
            val gjennomsnitt = gUnits.summer()
            return GUnit(gjennomsnitt.verdi.divide(BigDecimal(gUnits.size), RoundingMode.HALF_UP))
        }

        private fun Iterable<GUnit>.summer(): GUnit {
            return GUnit(this.sumOf { gUnit -> gUnit.verdi })
        }
    }

    fun verdi(): BigDecimal {
        return verdi
    }

    fun pluss(addend: GUnit): GUnit {
        return GUnit(this.verdi + addend.verdi)
    }

    fun multiplisert(faktor: Prosent): GUnit {
        return GUnit(faktor.multiplisert(this.verdi))
    }

    fun multiplisert(faktor: Beløp): Beløp {
        return faktor.multiplisert(this.verdi)
    }

    fun multiplisert(faktor: Int): GUnit {
        return GUnit(this.verdi.multiply(BigDecimal(faktor)))
    }

    fun dividert(nevner: Prosent): GUnit {
        return GUnit(
            Prosent.dividert(
                teller = this.verdi,
                nevner = nevner,
                scale = SCALE
            )
        )
    }

    fun dividert(nevner: Int): GUnit {
        return GUnit(this.verdi.divide(BigDecimal(nevner), SCALE, RoundingMode.HALF_UP))
    }

    fun toTredjedeler(): GUnit {
        return this.multiplisert(2).dividert(3)
    }

    fun begrensTil6GUnits(): GUnit {
        val begrensetVerdi = minOf(verdi, BigDecimal(6))
        return GUnit(begrensetVerdi)
    }

    override fun compareTo(other: GUnit): Int {
        return this.verdi.compareTo(other.verdi)
    }

    override fun toString(): String {
        return "GUnit(verdi=$verdi)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GUnit

        return verdi == other.verdi
    }

    override fun hashCode(): Int {
        return verdi.hashCode()
    }
}
