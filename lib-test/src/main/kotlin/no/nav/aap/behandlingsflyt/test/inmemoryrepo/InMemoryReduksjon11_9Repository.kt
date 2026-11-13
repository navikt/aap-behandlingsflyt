package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Reduksjon11_9
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Reduksjon11_9Repository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryReduksjon11_9Repository : Reduksjon11_9Repository {
    private val reduksjoner = ConcurrentHashMap<BehandlingId, List<Reduksjon11_9>>()

    override fun hent(behandlingId: BehandlingId): List<Reduksjon11_9> = reduksjoner[behandlingId].orEmpty()

    override fun lagre(behandlingId: BehandlingId, reduksjoner: List<Reduksjon11_9>) {
        this.reduksjoner[behandlingId] = reduksjoner.toList()
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        hent(fraBehandling).takeIf { it.isNotEmpty() }?.let { lagre(tilBehandling, it) }
    }

    override fun slett(behandlingId: BehandlingId) {
        reduksjoner.remove(behandlingId)
    }
}
