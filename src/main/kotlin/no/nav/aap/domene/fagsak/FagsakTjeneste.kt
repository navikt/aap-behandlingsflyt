package no.nav.aap.domene.fagsak

import no.nav.aap.domene.person.Person
import java.util.UUID

object FagsakTjeneste {
    private var behandliger = HashMap<Long, Fagsak>()

    private val LOCK = Object()

    fun hent(fagsakId: Long): Fagsak {
        synchronized(LOCK) {
            return Fagsak(Person(UUID.randomUUID(), listOf()))
        }
    }

    fun opprett() {

    }
}
