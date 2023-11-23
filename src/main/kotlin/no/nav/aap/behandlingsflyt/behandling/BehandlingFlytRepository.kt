package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.steg.Tilstand

interface BehandlingFlytRepository {

    fun oppdaterBehandlingStatus(behandlingId: BehandlingId, status: Status)
    fun loggBesøktSteg(behandlingId: BehandlingId, tilstand: Tilstand)
}

fun BehandlingFlytRepository(connection: DBConnection): BehandlingFlytRepository {
    return BehandlingRepositoryImpl(connection)
}