package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sak.SakId
import java.util.*

interface BehandlingRepository {
    fun opprettBehandling(sakId: SakId, årsaker: List<Årsak>): Behandling
    fun finnSisteBehandlingFor(sakId: SakId): Behandling?
    fun hentAlleFor(sakId: SakId): List<Behandling>
    fun hent(behandlingId: BehandlingId): Behandling
    fun hent(referanse: UUID): Behandling
}

fun BehandlingRepository(connection: DBConnection): BehandlingRepository {
    return BehandlingRepositoryImpl(connection)
}