package no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger

import java.time.LocalDate
import java.time.temporal.ChronoUnit

class Fødselsdato(private val dato: LocalDate) {

    init {
        if (dato.isAfter(LocalDate.now())) throw IllegalArgumentException("Kan ikke sette fødselsdato inn i fremtiden")
    }

    fun alderPåDato(gittDato: LocalDate): Int {
        return dato.until(gittDato, ChronoUnit.YEARS).toInt()
    }

    fun toLocalDate(): LocalDate {
        return dato
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Fødselsdato

        return dato == other.dato
    }

    override fun hashCode(): Int {
        return dato.hashCode()
    }
}
