package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.komponenter.verdityper.Beløp
import java.time.Year

data class InntektPerÅrFraRegister(
    val år: Year,
    val beløp: Beløp
) {
    fun tilInntektPerÅr(): InntektPerÅr {
        return InntektPerÅr(år, beløp)
    }
}