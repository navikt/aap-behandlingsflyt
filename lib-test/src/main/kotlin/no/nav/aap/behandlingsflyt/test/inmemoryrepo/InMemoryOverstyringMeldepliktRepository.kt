package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryFactory

class InMemoryOverstyringMeldepliktRepository: OverstyringMeldepliktRepository {
    override fun hentHvisEksisterer(behandlingId: BehandlingId): OverstyringMeldepliktGrunnlag? {
        TODO("Not yet implemented")
    }

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: OverstyringMeldepliktVurdering
    ) {
        TODO("Not yet implemented")
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
        TODO("Not yet implemented")
    }

    companion object: RepositoryFactory<OverstyringMeldepliktRepository> {
        override fun konstruer(connection: DBConnection) = InMemoryOverstyringMeldepliktRepository()
    }
}