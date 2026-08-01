package no.nav.aap.barnetillegg

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.personopplysninger.Dødsdato
import no.nav.aap.personopplysninger.Fødselsdato

/**
 * Barn fra PDL. "Registerbarn"
 */
data class Barn(
    val ident: BarnIdentifikator,
    val fødselsdato: Fødselsdato,
    val dødsdato: Dødsdato? = null,
    val navn: String? = null
) : IBarn {
    override fun identifikator(): BarnIdentifikator {
        return ident
    }

    override fun fødselsdato(): Fødselsdato {
        return fødselsdato
    }

    companion object {
        /**
         * Returnerer perioden hvor barnet er mindre enn 18 år.
         */
        fun periodeMedRettTil(fødselsdato: Fødselsdato, dødsdato: Dødsdato?): Periode {
            val fom = fødselsdato.toLocalDate()
            val attenÅr = fom.plusYears(18).minusDays(1)
            val sluttDato = if (dødsdato != null) {
                minOf(attenÅr, dødsdato.toLocalDate())
            } else {
                attenÅr
            }
            return Periode(fom, sluttDato)
        }
    }
}

sealed interface IBarn {
    fun identifikator(): BarnIdentifikator

    fun fødselsdato(): Fødselsdato
}