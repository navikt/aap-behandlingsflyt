package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend.SykestipendGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend.SykestipendRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend.SykestipendVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryFactory
import java.util.concurrent.ConcurrentHashMap

class InMemorySykestipendRepository: SykestipendRepository {
    private val store = ConcurrentHashMap<BehandlingId, SykestipendGrunnlag>()

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: SykestipendVurdering
    ) {
        store[behandlingId] = SykestipendGrunnlag(vurdering)
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SykestipendGrunnlag? {
        return store[behandlingId]
    }

    override fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        store.remove(behandlingId)
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

    companion object: RepositoryFactory<SykestipendRepository> {
        override fun konstruer(connection: DBConnection) = InMemorySykestipendRepository()
    }
}