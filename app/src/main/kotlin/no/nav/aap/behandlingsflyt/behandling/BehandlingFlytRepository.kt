package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.Status
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

interface BehandlingFlytRepository {

    fun oppdaterBehandlingStatus(behandlingId: BehandlingId, status: Status)
    fun loggBesøktSteg(behandlingId: BehandlingId, tilstand: StegTilstand)
}

fun BehandlingFlytRepository(connection: DBConnection): BehandlingFlytRepository {
    return BehandlingRepositoryImpl(connection)
}