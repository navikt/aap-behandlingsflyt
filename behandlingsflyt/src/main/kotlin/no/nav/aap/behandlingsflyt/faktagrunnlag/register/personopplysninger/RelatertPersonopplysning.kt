package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person

data class RelatertPersonopplysning(
    private val person: Person,
    val fødselsdato: Fødselsdato,
    val dødsdato: Dødsdato? = null
) {

    fun gjelderForIdent(ident: Ident): Boolean {
        return person.identer().any { i -> i.identifikator == ident.identifikator }
    }

    fun ident(): Ident {
        return person.aktivIdent()
    }
}
