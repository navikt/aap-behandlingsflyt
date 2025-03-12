package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemorySamordningVurderingRepository : SamordningVurderingRepository {
    private val vurderinger = ConcurrentHashMap<BehandlingId, SamordningVurderingGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningVurderingGrunnlag? {
        val vurderinger = vurderinger[behandlingId]
        return vurderinger
    }

    override fun lagreVurderinger(behandlingId: BehandlingId, samordningVurderinger: SamordningVurderingGrunnlag) {
        vurderinger[behandlingId] = samordningVurderinger
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        TODO("Not yet implemented")
    }
}