package no.nav.aap.behandlingsflyt.faktagrunnlag.beregning.år

import java.time.LocalDate

data class Input(val nedsettelsesDato: LocalDate, val ytterligereNedsettelsesDato: LocalDate? = null) {

    fun datoerForInnhenting(): Set<LocalDate> {
        if (ytterligereNedsettelsesDato == null) {
            return setOf(nedsettelsesDato)
        }

        return setOf(nedsettelsesDato, ytterligereNedsettelsesDato)
    }
}
