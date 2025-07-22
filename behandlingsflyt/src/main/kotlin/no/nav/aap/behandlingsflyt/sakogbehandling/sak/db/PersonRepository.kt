package no.nav.aap.behandlingsflyt.sakogbehandling.sak.db

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.lookup.repository.Repository
import java.util.*

interface PersonRepository : Repository {
    fun finnEllerOpprett(identer: List<Ident>): Person
    fun hent(identifikator: UUID): Person
    fun hent(personId: Long): Person
    fun finn(ident: Ident): Person?
}