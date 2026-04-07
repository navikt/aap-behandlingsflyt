package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryMeldepliktRepository : MeldepliktRepository {
    override fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktGrunnlag? {
        TODO("Not yet implemented")
    }

    override fun lagre(
        behandlingId: BehandlingId,
        vurderinger: List<Fritaksvurdering>
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
}