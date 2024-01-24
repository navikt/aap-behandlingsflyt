package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import java.util.*

interface BehandlingRepository {
    fun opprettBehandling(sakId: SakId, årsaker: List<Årsak>, typeBehandling: TypeBehandling): Behandling
    fun finnSisteBehandlingFor(sakId: SakId): Behandling?
    fun hentAlleFor(sakId: SakId): List<Behandling>
    fun hent(behandlingId: BehandlingId): Behandling
    fun hent(referanse: UUID): Behandling
}

fun behandlingRepository(connection: DBConnection): BehandlingRepository {
    return BehandlingRepositoryImpl(connection)
}