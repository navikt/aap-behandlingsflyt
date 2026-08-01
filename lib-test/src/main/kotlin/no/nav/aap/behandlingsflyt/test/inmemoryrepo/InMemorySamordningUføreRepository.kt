package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.samordning.SamordningUføreGrunnlag
import no.nav.aap.samordning.SamordningUføreVurdering
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.steg.samordning.uførevurdering.SamordningUføreRepository
import java.util.concurrent.ConcurrentHashMap

object InMemorySamordningUføreRepository : SamordningUføreRepository {
    private val store = ConcurrentHashMap<BehandlingId, SamordningUføreGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningUføreGrunnlag? {
        return store[behandlingId]
    }

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: SamordningUføreVurdering
    ) {
        store[behandlingId] = SamordningUføreGrunnlag(vurdering)
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        store[fraBehandling]?.let { store[tilBehandling] = it }
    }

    override fun slett(behandlingId: BehandlingId) {
        store.remove(behandlingId)
    }
}