package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryFactory
import java.util.concurrent.ConcurrentHashMap

class InMemorySamordningUføreRepository : SamordningUføreRepository {
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

    companion object : RepositoryFactory<SamordningUføreRepository> {
        override fun konstruer(connection: DBConnection) = InMemorySamordningUføreRepository()
    }
}