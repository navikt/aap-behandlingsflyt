package no.nav.aap.motor.retry

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbType

class DriftJobbRepositoryExposed(connection: DBConnection) {
    private val retryFeiledeOppgaverRepository = RetryFeiledeJobberRepository(connection)

    fun markerAlleFeiledeForKlare(): Int {
        return retryFeiledeOppgaverRepository.markerAlleFeiledeForKlare()
    }

    fun markerFeilendeForKlar(jobbId: Long): Int {
        return retryFeiledeOppgaverRepository.markerFeiledeForKlare(jobbId)
    }

    fun hentAlleFeilende(): List<Pair<JobbInput, String?>> {
        return retryFeiledeOppgaverRepository.hentAlleFeilede()
    }

    fun hentInfoOmGjentagendeJobber(): List<JobbInput> {
        return JobbType.cronTypes().map { retryFeiledeOppgaverRepository.hentInfoOmSisteAvType(it) }
    }

    fun hentSisteJobber(antall: Int): List<Pair<JobbInput, String?>> {
        return retryFeiledeOppgaverRepository.hentInfoOmSiste(antall)
    }
}
