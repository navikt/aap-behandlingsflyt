package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import java.time.LocalDate

class Dødsdato(private val dato: LocalDate) {

    companion object {
        fun parse(dødsdato: CharSequence): Dødsdato {
            return Dødsdato(LocalDate.parse(dødsdato))
        }
    }

    fun toLocalDate() = dato

}