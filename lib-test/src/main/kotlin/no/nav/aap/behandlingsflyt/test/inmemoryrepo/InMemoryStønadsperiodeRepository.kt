package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryStønadsperiodeRepository : StønadsperiodeRepository {
    private val store = ConcurrentHashMap<BehandlingId, StønadsperiodeGrunnlag>()

    fun reset() = store.clear()

    override fun lagre(behandlingId: BehandlingId, vurderinger: Set<StønadsperiodeVurdering>) {
        store[behandlingId] = StønadsperiodeGrunnlag(vurderinger)
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): StønadsperiodeGrunnlag? =
        store[behandlingId]

    override fun tilbakestillGrunnlag(behandlingId: BehandlingId, forrigeBehandling: BehandlingId?) {
        store.remove(behandlingId)
        if (forrigeBehandling != null) {
            kopier(forrigeBehandling, behandlingId)
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        store[fraBehandling]?.let { store[tilBehandling] = it }
    }

    override fun slett(behandlingId: BehandlingId) {
        store.remove(behandlingId)
    }
}
