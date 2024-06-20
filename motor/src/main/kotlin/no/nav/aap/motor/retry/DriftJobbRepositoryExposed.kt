package no.nav.aap.motor.retry

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class DriftJobbRepositoryExposed(connection: DBConnection) {
    private val retryFeiledeOppgaverRepository = RetryFeiledeJobberRepository(connection)

    fun markerAlleFeiledeForKlare(): Int {
        return retryFeiledeOppgaverRepository.markerAlleFeiledeForKlare()
    }

    fun markerFeilendeForKlar(behandlingId: BehandlingId): Int {
        return retryFeiledeOppgaverRepository.markerFeiledeForKlare(behandlingId)
    }

    fun hentAlleFeilende(): List<Pair<JobbInput, String>> {
        return retryFeiledeOppgaverRepository.hentAlleFeilede()
    }
}
