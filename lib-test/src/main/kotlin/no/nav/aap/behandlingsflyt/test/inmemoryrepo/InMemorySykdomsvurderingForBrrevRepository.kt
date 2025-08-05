package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.util.concurrent.ConcurrentHashMap

object InMemorySykdomsvurderingForBrrevRepository : SykdomsvurderingForBrevRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, SykdomsvurderingForBrev>()

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: SykdomsvurderingForBrev
    ) {
        grunnlag[behandlingId] = vurdering
    }

    override fun hent(behandlingId: BehandlingId): SykdomsvurderingForBrev? {
        return grunnlag[behandlingId]
    }

    override fun hent(sakId: SakId): List<SykdomsvurderingForBrev> {
        TODO("Not yet implemented")
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
    }
}