package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryUføreRepository : UføreRepository {
    private val mutex = Any()
    private val grunnlag = HashMap<BehandlingId, UføreGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId) = synchronized(mutex) {
        grunnlag[behandlingId]
    }

    override fun hentEldsteGrunnlag(behandlingId: BehandlingId) = synchronized(mutex) {
        grunnlag[behandlingId]
    }

    override fun lagre(
        behandlingId: BehandlingId,
        uføre: Set<no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre>
    ) = synchronized(mutex) {
        grunnlag[behandlingId] = UføreGrunnlag(behandlingId, uføre)
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) = synchronized(mutex) {
        val fraGrunnlag = grunnlag[fraBehandling]
        if (fraGrunnlag != null) {
            grunnlag[tilBehandling] = fraGrunnlag.copy(behandlingId = tilBehandling)
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(mutex) {
            grunnlag.remove(behandlingId)
        }
    }
}
