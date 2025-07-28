package no.nav.aap.behandlingsflyt.sakogbehandling.sak.db

import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.lookup.repository.Repository

interface PersonRepository : Repository {
    /**
     * Finn [Person] med de gitte identene. Om det er en ny ident i listen siden forrige lagring,
     * oppdateres Person-objektet med den nye identen.
     */
    fun finnEllerOpprett(identer: List<Ident>): Person
    fun hent(personId: Long): Person
    fun finn(ident: Ident): Person?
}