package no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.type.Periode

data class Barn(val ident: Ident, val fødselsdato: Fødselsdato, val dødsdato: Dødsdato? = null) {
    companion object {
        fun periodeMedRettTil(fødselsdato: Fødselsdato): Periode {
            val fom = fødselsdato.toLocalDate()
            // TODO: ta hensyn til dødsdato hvis den er satt (Fredrik)
            return Periode(fom, fom.plusYears(18).minusDays(1))
        }
    }
}