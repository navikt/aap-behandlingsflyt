package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.inntektsbortfall.InntektsbortfallRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.InntektsbortfallVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryInntektsbortfallRepository : InntektsbortfallRepository {
    private val vurderinger = ConcurrentHashMap<BehandlingId, InntektsbortfallVurdering>()

    override fun lagre(behandlingId: BehandlingId, vurdering: InntektsbortfallVurdering) {
        vurderinger[behandlingId] = vurdering
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): InntektsbortfallVurdering? =
        vurderinger[behandlingId]

    override fun deaktiverGjeldendeVurdering(behandlingId: BehandlingId) {
        vurderinger.remove(behandlingId)
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        vurderinger[fraBehandling]?.let { vurderinger[tilBehandling] = it }
    }

    override fun slett(behandlingId: BehandlingId) {
        vurderinger.remove(behandlingId)
    }
}
