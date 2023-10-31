package no.nav.aap.behandlingsflyt.sak

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection

class SakService(connection: DBConnection) {

    private val sakRepository = SakRepository(connection)

    fun hent(sakId: SakId): Sak {
        return sakRepository.hent(sakId)
    }
    fun hent(saksnummer: Saksnummer): Sak {
        return sakRepository.hent(saksnummer)
    }
}
