package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.KansellerRevurderingGrunnlag
import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.KansellerRevurderingRepository
import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.KansellerRevurderingVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryKansellerRevurderingRepository : KansellerRevurderingRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, KansellerRevurderingGrunnlag>()

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: KansellerRevurderingVurdering
    ) {
        grunnlag[behandlingId] = KansellerRevurderingGrunnlag(
            vurdering = vurdering
        )
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): KansellerRevurderingGrunnlag? {
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