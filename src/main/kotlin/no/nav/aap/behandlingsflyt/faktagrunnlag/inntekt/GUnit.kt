package no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Faktor av antall G for representasjon av størrelsen på det maksnimalet grunnlaget
 */
class GUnit(private val verdi: BigDecimal) {

    init {
        verdi.setScale(10, RoundingMode.HALF_UP)
    }

    fun verdi(): BigDecimal {
        return verdi
    }
}