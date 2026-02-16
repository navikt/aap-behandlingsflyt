package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryVedtakslengdeRepository: VedtakslengdeRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, VedtakslengdeGrunnlag>()
    private val lock = Any()
    
    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: VedtakslengdeVurdering
    ) {
        synchronized(lock) {
            grunnlag[behandlingId] = VedtakslengdeGrunnlag(vurdering)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): VedtakslengdeGrunnlag? {
        return synchronized(lock) {
            grunnlag[behandlingId]
        }
    }

    override fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        TODO("Not yet implemented")
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        synchronized(lock) {
            grunnlag.put(tilBehandling, grunnlag.getValue(fraBehandling))
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            grunnlag.remove(behandlingId)
        }
    }
}