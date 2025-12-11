package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class Fødselsdato(val dato: LocalDate) {

    init {
        if (dato.isAfter(LocalDate.now())) throw IllegalArgumentException("Kan ikke sette fødselsdato inn i fremtiden")
    }

    fun alderPåDato(gittDato: LocalDate): Int {
        return dato.until(gittDato, ChronoUnit.YEARS).toInt()
    }

    fun alderMedMånederPåDato(gittDato: LocalDate): AlderMedMåneder {
        val år = dato.until(gittDato, ChronoUnit.YEARS).toInt()
        val måneder = dato.plusYears(år.toLong()).until(gittDato, ChronoUnit.MONTHS).toInt()
        return AlderMedMåneder(år, måneder)
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

    companion object {
        fun parse(fødselsdato: String): Fødselsdato {
            return Fødselsdato(LocalDate.parse(fødselsdato))
        }
    }
}

data class AlderMedMåneder(val år: Int, val måneder: Int): Comparable<AlderMedMåneder> {
    override fun toString(): String {
        return "$år år og $måneder måneder"
    }
    
    override fun compareTo(other: AlderMedMåneder): Int {
        return when {
            this.år != other.år -> this.år - other.år
            else -> this.måneder - other.måneder
        }
    }
}