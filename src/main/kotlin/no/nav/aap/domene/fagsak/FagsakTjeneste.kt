package no.nav.aap.domene.fagsak

import no.nav.aap.domene.person.Person
import no.nav.aap.domene.typer.Periode
import no.nav.aap.domene.typer.Saksnummer

object FagsakTjeneste {
    private var fagsaker = HashMap<Long, Fagsak>()

    private val LOCK = Object()

    fun hent(fagsakId: Long): Fagsak {
        synchronized(LOCK) {
            return fagsaker.getValue(fagsakId)
        }
    }

    fun hent(saksnummer: Saksnummer): Fagsak {
        synchronized(LOCK) {
            return fagsaker.values.filter { it.saksnummer == saksnummer }.first()
        }
    }

    fun opprett(person: Person, periode: Periode): Fagsak {
        synchronized(LOCK) {
            if (fagsaker.values.any { sak -> sak.person == person && sak.rettighetsperiode.overlapper(periode) }) {
                throw IllegalArgumentException("Forsøker å opprette sak når det finnes en som overlapper")
            }
            val fagsak = Fagsak(fagsaker.keys.size.plus(10000001L), person, periode)
            fagsaker[fagsak.id] = fagsak

            return fagsak
        }
    }

    fun finnEllerOpprett(person: Person, periode: Periode): Fagsak {
        synchronized(LOCK) {
            val relevanteFagsaker =
                fagsaker.values.filter { sak -> sak.person == person && sak.rettighetsperiode.overlapper(periode) }

            if (relevanteFagsaker.isEmpty()) {
                return opprett(person, periode)
            }

            if (relevanteFagsaker.size != 1) {
                throw IllegalStateException("Fant flere saker som er relevant: " + relevanteFagsaker)
            }
            return relevanteFagsaker.first()
        }
    }
}
