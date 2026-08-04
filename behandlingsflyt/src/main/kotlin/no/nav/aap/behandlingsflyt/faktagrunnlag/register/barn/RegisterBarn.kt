package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import java.time.LocalDate

data class RegisterBarn(val id: Long, val barn: List<Barn>)

fun List<Barn>.filtrerBortMigrerteBarn(): List<Barn> {
    return this.filterNot { barn ->
        // Filtrer bort barn som er migrert fra dsf (Det Sentrale Folkeregister)
        barn.navn?.startsWith("migrert fra dsf", ignoreCase = true) == true &&
                this.any { annetBarn ->
                    annetBarn != barn &&
                            annetBarn.fødselsdato == barn.fødselsdato &&
                            annetBarn.ident.hentIdent() != null
                }
    }
}

fun List<Barn>.filtrerBortBarnEldreEnnBruker(brukerFødselsdato: LocalDate): List<Barn> {
    return this.filter { barn ->
        val barnFødselsdato = barn.fødselsdato.toLocalDate()
        barnFødselsdato.isAfter(brukerFødselsdato)
    }
}