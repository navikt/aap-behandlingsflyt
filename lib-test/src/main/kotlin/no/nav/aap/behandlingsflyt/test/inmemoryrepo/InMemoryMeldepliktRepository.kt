package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
object InMemoryMeldepliktRepository : MeldepliktRepository {
    private val meldepliktGrunnlag = mutableMapOf<BehandlingId, MeldepliktGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktGrunnlag? =
        meldepliktGrunnlag[behandlingId]

    override fun lagre(
        behandlingId: BehandlingId,
        vurderinger: List<Fritaksvurdering>
    ) {
        meldepliktGrunnlag[behandlingId] = MeldepliktGrunnlag(vurderinger)
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        meldepliktGrunnlag[fraBehandling]?.let { meldepliktGrunnlag[tilBehandling] = it }
    }

    override fun slett(behandlingId: BehandlingId) {
        meldepliktGrunnlag.remove(behandlingId)
    }
}