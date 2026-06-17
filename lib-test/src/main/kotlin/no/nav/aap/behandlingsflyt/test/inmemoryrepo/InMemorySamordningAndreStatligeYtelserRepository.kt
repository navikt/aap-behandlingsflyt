package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryFactory
import java.util.concurrent.ConcurrentHashMap

class InMemorySamordningAndreStatligeYtelserRepository: SamordningAndreStatligeYtelserRepository {
    private val store = ConcurrentHashMap<BehandlingId, SamordningAndreStatligeYtelserGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningAndreStatligeYtelserGrunnlag? {
        return store[behandlingId]
    }

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: SamordningAndreStatligeYtelserVurdering
    ) {
        store[behandlingId] = SamordningAndreStatligeYtelserGrunnlag(vurdering)
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

    companion object: RepositoryFactory<SamordningAndreStatligeYtelserRepository> {
        override fun konstruer(connection: DBConnection) =  InMemorySamordningAndreStatligeYtelserRepository()
    }
}