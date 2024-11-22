package no.nav.aap.behandlingsflyt.sakogbehandling.sak.db

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.util.*

interface PersonRepository {
    fun finnEllerOpprett(identer: List<Ident>): Person
    fun oppdater(person: Person, identer: List<Ident>)
    fun hent(identifikator: UUID): Person
    fun hent(personId: Long): Person
    fun finn(ident: Ident): Person?
}