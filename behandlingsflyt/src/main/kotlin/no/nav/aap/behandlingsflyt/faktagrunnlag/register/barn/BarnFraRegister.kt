package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.komponenter.type.Periode

data class BarnFraRegister(val ident: Ident, val fødselsdato: Fødselsdato, val dødsdato: Dødsdato? = null) :
    BarnMedIdent {
    override fun identifikator(): BarnIdentifikator {
        return ident.let { BarnIdentifikator.BarnIdent(it) }
    }

    override fun fødselsdato(): Fødselsdato {
        return fødselsdato
    }

    companion object {
        /**
         * Returnerer perioden hvor barnet er mindre enn 18 år.
         */
        fun periodeMedRettTil(fødselsdato: Fødselsdato): Periode {
            val fom = fødselsdato.toLocalDate()
            // TODO: ta hensyn til dødsdato hvis den er satt (Fredrik)
            return Periode(fom, fom.plusYears(18).minusDays(1))
        }
    }
}

data class LagretBarnFraRegister(val personId: PersonId, val fødselsdato: Fødselsdato, val dødsdato: Dødsdato? = null) :
    BarnMedIdent {
    override fun identifikator(): BarnIdentifikator {
        return BarnIdentifikator.RegistertBarnPerson(personId)
    }

    override fun fødselsdato(): Fødselsdato {
        return fødselsdato
    }

}

sealed interface BarnMedIdent : IBarn

sealed interface IBarn {
    fun identifikator(): BarnIdentifikator

    fun fødselsdato(): Fødselsdato
}