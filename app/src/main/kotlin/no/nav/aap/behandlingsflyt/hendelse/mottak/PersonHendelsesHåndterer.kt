package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.sakogbehandling.Ident

class PersonHendelsesHåndterer(
    private val connection: DBConnection
) {
    suspend fun håndtere(key: Ident, hendelse: PersonHendelse): Saksnummer {
        return PersonOgSakService(connection).finnEllerOpprett(key, hendelse.periode())
    }
}
