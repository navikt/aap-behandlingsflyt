package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import com.fasterxml.jackson.annotation.JsonValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class Fødselsdato(private val dato: LocalDate) {

    init {
        if (dato.isAfter(LocalDate.now())) throw IllegalArgumentException("Kan ikke sette fødselsdato inn i fremtiden")
    }

    fun alderPåDato(gittDato: LocalDate): Int {
        return dato.until(gittDato, ChronoUnit.YEARS).toInt()
    }

    fun `25årsDagen`(): LocalDate {
        return dato.plusYears(25)
    }

    fun toLocalDate(): LocalDate {
        return dato
    }

    fun toFormattedString(): String {
        return dato.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    @JsonValue
    fun jsonValue(): LocalDate {
        return dato
    }

    companion object {
        fun parse(fødselsdato: String): Fødselsdato {
            return Fødselsdato(LocalDate.parse(fødselsdato))
        }
    }
}
