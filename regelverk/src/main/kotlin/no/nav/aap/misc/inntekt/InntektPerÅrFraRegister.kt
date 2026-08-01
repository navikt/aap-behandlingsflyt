package no.nav.aap.misc.inntekt

import java.time.Year
import no.nav.aap.beregning.InntektPerÅr
import no.nav.aap.komponenter.verdityper.Beløp

data class InntektPerÅrFraRegister(
    val år: Year,
    val beløp: Beløp
) {
    fun tilInntektPerÅr(): InntektPerÅr {
        return InntektPerÅr(år, beløp)
    }
}