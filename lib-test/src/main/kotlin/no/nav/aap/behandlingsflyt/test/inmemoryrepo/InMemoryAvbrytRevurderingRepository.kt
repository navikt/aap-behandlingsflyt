package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingGrunnlag
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingRepository
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryAvbrytRevurderingRepository : AvbrytRevurderingRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, AvbrytRevurderingGrunnlag>()

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: AvbrytRevurderingVurdering
    ) {
        grunnlag[behandlingId] = AvbrytRevurderingGrunnlag(
            vurdering = vurdering
        )
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): AvbrytRevurderingGrunnlag? {
        return grunnlag[behandlingId]
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
        TODO("Not yet implemented")
    }
}