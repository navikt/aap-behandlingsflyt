package no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt

import java.math.BigDecimal
import java.math.RoundingMode

class Beløp(verdi: BigDecimal) {
    private val verdi = verdi.setScale(2, RoundingMode.HALF_UP)

    fun verdi(): BigDecimal {
        return verdi
    }

    fun deltPå(nevner: Beløp): BigDecimal {
        return this.verdi.divide(nevner.verdi, RoundingMode.HALF_UP)
    }
}
