package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryVurderRettighetsperiodeRepository : VurderRettighetsperiodeRepository {
    private val vurderinger = ConcurrentHashMap<BehandlingId, RettighetsperiodeVurdering>()

    override fun lagreVurdering(behandlingId: BehandlingId, vurdering: RettighetsperiodeVurdering?) {
        if (vurdering == null) {
            vurderinger.remove(behandlingId)
        } else {
            vurderinger[behandlingId] = vurdering
        }
    }

    override fun hentVurdering(behandlingId: BehandlingId): RettighetsperiodeVurdering? =
        vurderinger[behandlingId]

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        vurderinger[fraBehandling]?.let { vurderinger[tilBehandling] = it }
    }

    override fun slett(behandlingId: BehandlingId) {
        vurderinger.remove(behandlingId)
    }
}
