package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryFactory
import java.util.concurrent.ConcurrentHashMap

class InMemoryBarnepensjonRepository: BarnepensjonRepository {
    private val store = ConcurrentHashMap<BehandlingId, BarnepensjonGrunnlag>()

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: BarnepensjonVurdering
    ) {
        store[behandlingId] = BarnepensjonGrunnlag(vurdering)
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): BarnepensjonGrunnlag? {
        return store[behandlingId]
    }

    override fun hentHistoriskeVurderinger(
        sakId: SakId,
        behandlingId: BehandlingId
    ): List<BarnepensjonGrunnlag> {
        return listOfNotNull(store[behandlingId])
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

    companion object: RepositoryFactory<BarnepensjonRepository> {
        override fun konstruer(connection: DBConnection ) = InMemoryBarnepensjonRepository()
    }
}