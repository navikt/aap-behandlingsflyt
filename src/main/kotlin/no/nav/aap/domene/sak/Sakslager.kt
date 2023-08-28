package no.nav.aap.domene.sak

import no.nav.aap.domene.person.Person
import no.nav.aap.domene.typer.Periode
import no.nav.aap.domene.typer.Saksnummer

object Sakslager {
    private var saker = HashMap<Long, Sak>()

    private val LOCK = Object()

    fun hent(sakId: Long): Sak {
        synchronized(LOCK) {
            return saker.getValue(sakId)
        }
    }

    fun hent(saksnummer: Saksnummer): Sak {
        synchronized(LOCK) {
            return saker.values.first { it.saksnummer == saksnummer }
        }
    }

    private fun opprett(person: Person, periode: Periode): Sak {
        synchronized(LOCK) {
            if (saker.values.any { sak -> sak.person == person && sak.rettighetsperiode.overlapper(periode) }) {
                throw IllegalArgumentException("Forsøker å opprette sak når det finnes en som overlapper")
            }
            val sak = Sak(saker.keys.size.plus(10000001L), person, periode)
            saker[sak.id] = sak

            return sak
        }
    }

    fun finnEllerOpprett(person: Person, periode: Periode): Sak {
        synchronized(LOCK) {
            val relevantesaker =
                saker.values.filter { sak -> sak.person == person && sak.rettighetsperiode.overlapper(periode) }

            if (relevantesaker.isEmpty()) {
                return opprett(person, periode)
            }

            if (relevantesaker.size != 1) {
                throw IllegalStateException("Fant flere saker som er relevant: $relevantesaker")
            }
            return relevantesaker.first()
        }
    }

    fun finnSakerFor(person: Person): List<Sak> {
        synchronized(LOCK) {
            return saker.values.filter { sak -> sak.person == person }.toList()
        }
    }

    fun finnAlle(): List<Sak> {
        synchronized(LOCK) {
            return saker.values.toList()
        }
    }
}
