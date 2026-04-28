package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonskravVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryFactory
import java.util.concurrent.ConcurrentHashMap

class InMemoryTjenestepensjonRefusjonsKravVurderingRepository: TjenestepensjonRefusjonsKravVurderingRepository {
    private val store = ConcurrentHashMap<BehandlingId, TjenestepensjonRefusjonskravVurdering>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): TjenestepensjonRefusjonskravVurdering? {
        return store[behandlingId]
    }

    override fun hent(behandlingId: BehandlingId): TjenestepensjonRefusjonskravVurdering {
        return store[behandlingId]!!
    }

    override fun lagre(
        sakId: SakId,
        behandlingId: BehandlingId,
        vurdering: TjenestepensjonRefusjonskravVurdering
    ) {
        store[behandlingId] = vurdering
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

    companion object: RepositoryFactory<TjenestepensjonRefusjonsKravVurderingRepository> {
        override fun konstruer(connection: DBConnection) = InMemoryTjenestepensjonRefusjonsKravVurderingRepository()
    }
}