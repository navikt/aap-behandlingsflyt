package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryKravRepository : KravRepository {
    private val store = ConcurrentHashMap<BehandlingId, KravGrunnlag>()

    override fun lagre(behandlingId: BehandlingId, vurderinger: Set<KravVurdering>) {
        store[behandlingId] = KravGrunnlag(vurderinger)
    }

    override fun hent(behandlingId: BehandlingId): KravGrunnlag =
        store[behandlingId] ?: error("Ingen kravgrunnlag for $behandlingId")

    override fun hentHvisEksisterer(behandlingId: BehandlingId): KravGrunnlag? =
        store[behandlingId]

    override fun slett(behandlingId: BehandlingId) {
        store.remove(behandlingId)
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        store[fraBehandling]?.let { store[tilBehandling] = it }
    }
}
