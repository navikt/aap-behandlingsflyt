package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.PersonRepository
import java.util.*

object InMemoryPersonRepository: PersonRepository {
    val personer = mutableMapOf<Long, Person>()
    override fun finnEllerOpprett(identer: List<Ident>): Person {
        val id = personer.size.toLong()
        val person = Person(id, UUID.randomUUID(), identer)
        personer[id] = person
        return person
    }

    override fun oppdater(person: Person, identer: List<Ident>) {
        personer[person.id] = Person(person.id, person.identifikator, identer)
    }

    override fun hent(identifikator: UUID): Person {
        return personer.values.single { it.identifikator == identifikator }
    }

    override fun hent(personId: Long): Person {
        return personer[personId]!!
    }

    override fun finn(ident: Ident): Person? {
        return personer.values.find { it.identer().contains(ident) }
    }
}