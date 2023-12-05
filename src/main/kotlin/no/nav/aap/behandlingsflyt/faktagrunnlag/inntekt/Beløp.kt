package no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt

import java.math.BigDecimal
import java.math.RoundingMode

class Beløp(verdi: BigDecimal) {
    private val verdi = verdi.setScale(2, RoundingMode.HALF_UP)

    constructor(intVerdi: Int) : this(BigDecimal(intVerdi))
    constructor(stringVerdi: String) : this(BigDecimal(stringVerdi))

    fun verdi(): BigDecimal {
        return verdi
    }

    fun divitert(nevner: Beløp, scale: Int = 10): BigDecimal {
        return this.verdi.divide(nevner.verdi, scale, RoundingMode.HALF_UP)
    }
}
