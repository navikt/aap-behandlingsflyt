package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
object InMemoryMeldepliktRepository : MeldepliktRepository {
    private val tomtMeldepliktGrunnlag = mutableMapOf<BehandlingId, MeldepliktGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktGrunnlag? =
        tomtMeldepliktGrunnlag[behandlingId]

    override fun lagre(
        behandlingId: BehandlingId,
        vurderinger: List<Fritaksvurdering>
    ) {
        tomtMeldepliktGrunnlag[behandlingId] = MeldepliktGrunnlag(vurderinger)
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        tomtMeldepliktGrunnlag[fraBehandling]?.let { tomtMeldepliktGrunnlag[tilBehandling] = it }
    }

    override fun slett(behandlingId: BehandlingId) {
        tomtMeldepliktGrunnlag.remove(behandlingId)
    }
}