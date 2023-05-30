package no.nav.aap.domene.fagsak

import no.nav.aap.domene.person.Person
import no.nav.aap.domene.typer.Ident
import no.nav.aap.domene.typer.Periode
import java.util.UUID

object FagsakTjeneste {
    private var fagsaker = HashMap<Long, Fagsak>()

    private val LOCK = Object()

    fun hent(fagsakId: Long): Fagsak {
        synchronized(LOCK) {
            return fagsaker.getValue(fagsakId)
        }
    }

    fun opprett(ident: Ident, periode: Periode): Fagsak {
        synchronized(LOCK) {
            if (fagsaker.values.any { sak -> sak.person.er(ident) && sak.periode.overlapper(periode) }) {
                throw IllegalArgumentException("Forsøker å opprette sak når det finnes en som overlapper")
            }
            val fagsak = Fagsak(fagsaker.keys.size.plus(1L), Person(UUID.randomUUID(), listOf(ident)), periode)
            fagsaker[fagsak.id] = fagsak

            return fagsak
        }
    }

    fun finnEllerOpprett(ident: Ident, periode: Periode): Fagsak {
        synchronized(LOCK) {
            val relevanteFagsaker =
                fagsaker.values.filter { sak -> sak.person.er(ident) && sak.periode.overlapper(periode) }

            if (relevanteFagsaker.isEmpty()) {
                return opprett(ident, periode)
            }

            if (relevanteFagsaker.size != 1) {
                throw IllegalStateException("Fant flere saker som er relevant: " + relevanteFagsaker)
            }
            return relevanteFagsaker.first()
        }
    }
}
