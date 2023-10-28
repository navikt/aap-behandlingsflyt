package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection

class BehandlingService(connection: DBConnection) {

    private val behandlingRepository = BehandlingRepository(connection)

    fun hent(behandlingId: Long): Behandling {
        return behandlingRepository.hent(behandlingId)
    }
}