package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Dødsdato(private val dato: LocalDate) {

    companion object {
        fun parse(dødsdato: CharSequence): Dødsdato {
            return Dødsdato(LocalDate.parse(dødsdato))
        }
    }

    fun toLocalDate() = dato

    fun toFormatedString(): String {
        return dato.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}