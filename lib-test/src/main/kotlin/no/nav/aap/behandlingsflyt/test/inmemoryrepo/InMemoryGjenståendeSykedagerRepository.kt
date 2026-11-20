package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.GjenståendeSykedagerRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.GjenståendeSykedager
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryGjenståendeSykedagerRepository : GjenståendeSykedagerRepository {
    private data class Rad(val data: GjenståendeSykedager, val aktiv: Boolean)

    private val rader = ConcurrentHashMap<BehandlingId, List<Rad>>()
    private val lock = Any()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): GjenståendeSykedager? {
        synchronized(lock) {
            return rader[behandlingId]?.firstOrNull { it.aktiv }?.data
        }
    }

    override fun lagre(behandlingId: BehandlingId, gjenståendeSykedager: GjenståendeSykedager) {
        synchronized(lock) {
            val eksisterende = rader[behandlingId].orEmpty()
            if (eksisterende.firstOrNull { it.aktiv }?.data == gjenståendeSykedager) return
            rader[behandlingId] = eksisterende.map { it.copy(aktiv = false) } + Rad(gjenståendeSykedager, true)
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        synchronized(lock) {
            val aktiv = rader[fraBehandling]?.firstOrNull { it.aktiv } ?: return
            rader[tilBehandling] = rader[tilBehandling].orEmpty().map { it.copy(aktiv = false) } + aktiv
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            rader.remove(behandlingId)
        }
    }
}
