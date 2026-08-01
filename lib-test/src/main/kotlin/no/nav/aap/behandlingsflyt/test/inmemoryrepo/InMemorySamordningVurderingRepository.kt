package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.misc.SamordningVurderingGrunnlag
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.steg.samordning.ytelsevurdering.SamordningVurderingRepository
import java.util.concurrent.*

object InMemorySamordningVurderingRepository : SamordningVurderingRepository {
    private val vurderinger = ConcurrentHashMap<BehandlingId, SamordningVurderingGrunnlag>()
    private val lock = Any()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningVurderingGrunnlag? {
        synchronized(lock) { return vurderinger[behandlingId] }
    }

    override fun lagreVurderinger(behandlingId: BehandlingId, samordningVurderinger: SamordningVurderingGrunnlag) {
        synchronized(lock) { vurderinger[behandlingId] = samordningVurderinger }
    }

    override fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        synchronized(lock) {
            vurderinger.remove(behandlingId)
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            vurderinger.remove(behandlingId)
        }
    }
}