package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.util.UUID

class RelatertPersonopplysning(
    private val person: Person,
    val fødselsdato: Fødselsdato,
    val dødsdato: Dødsdato? = null
) {
    fun personReferanse(): UUID {
        return person.identifikator
    }

    fun ident(): Ident {
        return person.aktivIdent()
    }
}
