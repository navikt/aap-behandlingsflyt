package no.nav.aap.verdityper

import java.math.BigDecimal
import java.math.RoundingMode

class Prosent private constructor(verdi: BigDecimal) : Comparable<Prosent> {
    //TODO: Hva skal scale være her? Hvor mange desimaler skal det være i prosenten?
    private val verdi = verdi.setScale(2, RoundingMode.HALF_UP)

    constructor(intVerdi: Int) : this(BigDecimal(intVerdi).divide(BigDecimal(100), 2, RoundingMode.HALF_UP))

    init {
        require(this.verdi >= BigDecimal(0)) { "Prosent kan ikke være negativ" }
        require(this.verdi <= BigDecimal(1)) { "Prosent kan ikke være større enn 100" }
    }

    companion object {
        val `0_PROSENT` = Prosent(0)
        val `30_PROSENT` = Prosent(30)
        val `50_PROSENT` = Prosent(50)
        val `66_PROSENT` = Prosent(66)
        val `70_PROSENT` = Prosent(70)
        val `100_PROSENT` = Prosent(100)

        internal fun dividert(teller: BigDecimal, nevner: Prosent, scale: Int = 10): BigDecimal {
            return teller.divide(nevner.verdi, scale, RoundingMode.HALF_UP)
        }
    }

    fun prosentverdi(): Int {
        return verdi.multiply(BigDecimal(100)).intValueExact()
    }

    fun justertFor(terskelverdi: Prosent): Prosent {
        if (this > terskelverdi) {
            return `100_PROSENT`
        }

        return this
    }

    fun komplement(): Prosent {
        return `100_PROSENT`.minus(this)
    }

    fun minus(subtrahend: Prosent): Prosent {
        return Prosent(this.verdi - subtrahend.verdi)
    }

    internal fun multiplisert(faktor: BigDecimal): BigDecimal {
        return this.verdi.multiply(faktor)
    }

    fun multiplisert(faktor: Beløp): Beløp {
        return faktor.multiplisert(this.verdi)
    }

    override fun compareTo(other: Prosent): Int {
        return this.verdi.compareTo(other.verdi)
    }

    override fun toString(): String {
        return "Prosent(${verdi * BigDecimal(100)})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Prosent

        return verdi == other.verdi
    }

    override fun hashCode(): Int {
        return verdi.hashCode()
    }
}
