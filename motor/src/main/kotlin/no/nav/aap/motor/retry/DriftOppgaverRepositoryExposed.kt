package no.nav.aap.motor.retry

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class DriftOppgaverRepositoryExposed(connection: DBConnection) {
    private val retryFeiledeOppgaverRepository = RetryFeiledeOppgaverRepository(connection)

    fun markerAlleFeiledeForKlare(): Int {
        return retryFeiledeOppgaverRepository.markerAlleFeiledeForKlare()
    }

    fun markerFeilendeForKlar(behandlingId: BehandlingId): Int {
        return retryFeiledeOppgaverRepository.markerFeiledeForKlare(behandlingId)
    }
}
