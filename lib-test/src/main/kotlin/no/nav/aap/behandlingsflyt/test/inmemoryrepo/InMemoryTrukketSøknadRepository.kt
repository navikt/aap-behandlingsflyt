package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryTrukketSøknadRepository : TrukketSøknadRepository {
    private val store = ConcurrentHashMap<BehandlingId, List<TrukketSøknadVurdering>>()

    override fun lagreTrukketSøknadVurdering(behandlingId: BehandlingId, vurdering: TrukketSøknadVurdering) {
        store.compute(behandlingId) { _, vurderinger ->
            vurderinger.orEmpty() + listOf(vurdering)
        }
    }

    override fun hentTrukketSøknadVurderinger(behandlingId: BehandlingId): List<TrukketSøknadVurdering> {
        return store[behandlingId].orEmpty()
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        store[tilBehandling] = store[fraBehandling].orEmpty()
    }

    override fun slett(behandlingId: BehandlingId) {
    }
}