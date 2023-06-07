package no.nav.aap.domene.person

import no.nav.aap.domene.typer.Ident
import java.util.UUID

object PersonTjeneste {
    private var personer = HashMap<UUID, Person>()

    private val LOCK = Object()

    fun finnEllerOpprett(ident: Ident): Person {
        synchronized(LOCK) {
            val relevantePersoner = personer.values.filter { person -> person.er(ident) }
            if (relevantePersoner.isNotEmpty()) {
                if (relevantePersoner.size > 1) {
                    throw IllegalStateException("Har flere personer knyttet til denne identen")
                }
                return relevantePersoner.first()
            }
            val person = Person(UUID.randomUUID(), listOf(ident))
            personer[person.identifikator] = person

            return person
        }
    }

    fun hent(identifikator: UUID): Person {
        synchronized(LOCK) {
            return personer.getValue(identifikator)
        }
    }
}
