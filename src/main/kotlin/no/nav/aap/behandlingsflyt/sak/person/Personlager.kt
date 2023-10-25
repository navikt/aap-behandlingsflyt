package no.nav.aap.behandlingsflyt.sak.person

import java.util.*

object Personlager {
    private var personer = HashMap<UUID, Person>() // Skal være en db eller noe liknende for persistens

    private val LOCK = Object()

    fun finnEllerOpprett(ident: Ident): Person {
        synchronized(LOCK) {
            // TODO: Kalle for å hente identer
            val relevantePersoner = personer.values.filter { person -> person.er(ident) }
            return if (relevantePersoner.isNotEmpty()) {
                if (relevantePersoner.size > 1) {
                    throw IllegalStateException("Har flere personer knyttet til denne identen")
                }
                relevantePersoner.first()
            } else {
                opprettPerson(ident)
            }
        }
    }

    fun hent(identifikator: UUID): Person {
        synchronized(LOCK) {
            return personer.getValue(identifikator)
        }
    }

    private fun opprettPerson(ident: Ident): Person {
        val person = Person(UUID.randomUUID(), listOf(ident))
        personer[person.identifikator] = person

        return person
    }

    fun finn(ident: Ident): Person? {
        synchronized(LOCK) {
            val relevantePersoner = personer.values.filter { person -> person.er(ident) }
            return if (relevantePersoner.isNotEmpty()) {
                relevantePersoner.first()
            } else {
                null
            }
        }
    }
}
