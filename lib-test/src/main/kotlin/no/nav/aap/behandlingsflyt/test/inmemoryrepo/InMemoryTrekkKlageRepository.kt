package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageGrunnlag
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageRepository
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryTrekkKlageRepository : TrekkKlageRepository {
    private val vurderinger = ConcurrentHashMap<BehandlingId, TrekkKlageVurdering>()

    override fun lagreTrekkKlageVurdering(
        behandlingId: BehandlingId,
        vurdering: TrekkKlageVurdering
    ) {
        vurderinger[behandlingId] = vurdering
    }

    override fun hentTrekkKlageGrunnlag(behandlingId: BehandlingId): TrekkKlageGrunnlag? {
        return vurderinger[behandlingId]?.let {
            TrekkKlageGrunnlag(vurdering = it)
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        // No-op in production implementation
    }

    override fun slett(behandlingId: BehandlingId) {
        // No-op in production implementation
    }
}