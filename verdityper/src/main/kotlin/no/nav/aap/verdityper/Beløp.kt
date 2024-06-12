package no.nav.aap.verdityper

import java.math.BigDecimal
import java.math.RoundingMode

class Beløp(verdi: BigDecimal) {
    val verdi = verdi.setScale(2, RoundingMode.HALF_UP)

    constructor(intVerdi: Int) : this(BigDecimal(intVerdi))
    constructor(stringVerdi: String) : this(BigDecimal(stringVerdi))
    constructor(longVerdi: Long) : this(BigDecimal(longVerdi))

    fun verdi(): BigDecimal {
        return verdi
    }

    fun pluss(beløp: Beløp): Beløp {
        return Beløp(this.verdi.add(beløp.verdi))
    }

    fun multiplisert(faktor: Int): Beløp {
        return Beløp(this.verdi.multiply(BigDecimal(faktor)))
    }

    internal fun multiplisert(faktor: BigDecimal): Beløp {
        return Beløp(this.verdi.multiply(faktor))
    }

    fun multiplisert(faktor: Prosent): Beløp {
        return faktor.multiplisert(this)
    }

    fun multiplisert(faktor: GUnit): Beløp {
        return faktor.multiplisert(this)
    }

    fun divitert(nevner: Beløp, scale: Int = 10): BigDecimal {
        return this.verdi.divide(nevner.verdi, scale, RoundingMode.HALF_UP)
    }

    fun dividert(nevner: Prosent): Beløp {
        return Beløp(Prosent.dividert(this.verdi, nevner))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Beløp

        return verdi == other.verdi
    }

    override fun hashCode(): Int {
        return verdi.hashCode()
    }

    override fun toString(): String {
        return "Beløp(verdi=$verdi)"
    }
}
