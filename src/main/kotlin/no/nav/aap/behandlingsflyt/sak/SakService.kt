package no.nav.aap.behandlingsflyt.sak

import no.nav.aap.behandlingsflyt.dbstuff.DbConnection

class SakService(connection: DbConnection) {

    private val sakRepository = SakRepository(connection)

    fun hent(sakId: Long): Sak {
        return sakRepository.hent(sakId)
    }
    fun hent(saksnummer: Saksnummer): Sak {
        return sakRepository.hent(saksnummer)
    }
}