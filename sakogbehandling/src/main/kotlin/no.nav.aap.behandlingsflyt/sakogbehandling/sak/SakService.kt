package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.verdityper.sakogbehandling.SakId

class SakService(connection: DBConnection) {

    private val sakRepository = SakRepositoryImpl(connection)

    fun hent(sakId: SakId): Sak {
        return sakRepository.hent(sakId)
    }

    fun hent(saksnummer: Saksnummer): Sak {
        return sakRepository.hent(saksnummer)
    }
}
