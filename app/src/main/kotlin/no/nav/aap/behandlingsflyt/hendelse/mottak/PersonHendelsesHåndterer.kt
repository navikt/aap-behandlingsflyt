package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PersonService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.sakRepository
import no.nav.aap.verdityper.sakogbehandling.Ident

class PersonHendelsesHåndterer(
    private val connection: DBConnection
) {
    suspend fun håndtere(key: Ident, hendelse: PersonHendelse): Saksnummer {
        val person = PersonService.hentPerson(key, connection)
        val sakRepository = sakRepository(connection)

        return sakRepository.finnEllerOpprett(person, hendelse.periode()).saksnummer
    }
}
