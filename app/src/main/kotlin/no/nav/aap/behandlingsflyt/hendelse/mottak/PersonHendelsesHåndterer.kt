package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sak.sakRepository

class PersonHendelsesHåndterer(connection: DBConnection) {

    private val personRepository = PersonRepository(connection)
    private val sakRepository = sakRepository(connection)

    fun håndtere(key: Ident, hendelse: PersonHendelse): Saksnummer {
        val person = personRepository.finnEllerOpprett(key)

        return sakRepository.finnEllerOpprett(person, hendelse.periode()).saksnummer
    }
}