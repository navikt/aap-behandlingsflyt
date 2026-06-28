package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Grunnlag
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Repository
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Vurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryAvslag11_27Repository : Avslag11_27Repository {
    private val store = ConcurrentHashMap<BehandlingId, List<Avslag11_27Vurdering>>()

    fun reset() = store.clear()

    override fun lagre(behandlingId: BehandlingId, vurderinger: List<Avslag11_27Vurdering>) {
        store[behandlingId] = vurderinger
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): Avslag11_27Grunnlag? {
        val vurderinger = store[behandlingId] ?: return null
        if (vurderinger.isEmpty()) return null
        return Avslag11_27Grunnlag(vurderinger)
    }

    override fun tilbakestillGrunnlag(behandlingId: BehandlingId, forrigeBehandling: BehandlingId?) {
        store.remove(behandlingId)
        forrigeBehandling?.let { store[behandlingId]?.let { v -> store[behandlingId] = v } }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        store[fraBehandling]?.let { store[tilBehandling] = it }
    }

    override fun slett(behandlingId: BehandlingId) {
        store.remove(behandlingId)
    }
}