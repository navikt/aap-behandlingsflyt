package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.type.Periode

data class Barn(val ident: Ident, val fødselsdato: Fødselsdato, val dødsdato: Dødsdato? = null) : IBarn {
    override fun identifikator(): BarnIdentifikator {
        return BarnIdentifikator.BarnIdent(ident)
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

sealed interface IBarn {
    fun identifikator(): BarnIdentifikator

    fun fødselsdato(): Fødselsdato
}