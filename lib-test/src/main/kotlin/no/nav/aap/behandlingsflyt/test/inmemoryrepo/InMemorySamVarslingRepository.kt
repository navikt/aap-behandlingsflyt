package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.datadeling.sam.LagretSamVarsling
import no.nav.aap.behandlingsflyt.datadeling.sam.SamVarsling
import no.nav.aap.behandlingsflyt.datadeling.sam.SamVarslingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.time.LocalDateTime

object InMemorySamVarslingRepository : SamVarslingRepository {
    private val varslinger = mutableMapOf<BehandlingId, List<LagretSamVarsling>>()
    private val lock = Any()

    override fun lagre(behandlingId: BehandlingId, varsling: SamVarsling) {
        synchronized(lock) {
            varslinger[behandlingId] = varslinger[behandlingId].orEmpty() + LagretSamVarsling(varsling, LocalDateTime.now())
        }
    }

    override fun hent(behandlingId: BehandlingId): List<LagretSamVarsling> {
        synchronized(lock) {
            return varslinger[behandlingId].orEmpty()
        }
    }
}
