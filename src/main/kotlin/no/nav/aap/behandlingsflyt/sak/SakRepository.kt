package no.nav.aap.behandlingsflyt.sak

import no.nav.aap.behandlingsflyt.domene.ElementNotFoundException
import no.nav.aap.behandlingsflyt.domene.Periode
import no.nav.aap.behandlingsflyt.sak.person.Person

object SakRepository {
    private var saker = HashMap<Long, Sak>()

    private val LOCK = Object()

    fun hent(sakId: Long): Sak {
        synchronized(LOCK) {
            return saker.getValue(sakId)
        }
    }

    fun hent(saksnummer: Saksnummer): Sak {
        synchronized(LOCK) {
            return saker.values.firstOrNull { it.saksnummer == saksnummer } ?: throw ElementNotFoundException()
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
