package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.komponenter.dbconnect.DBConnection

interface BehandlingFlytRepository {

    fun oppdaterBehandlingStatus(behandlingId: BehandlingId, status: Status)
    fun loggBesøktSteg(behandlingId: BehandlingId, tilstand: StegTilstand)
}

fun BehandlingFlytRepository(connection: DBConnection): BehandlingFlytRepository {
    return BehandlingRepositoryImpl(connection)
}