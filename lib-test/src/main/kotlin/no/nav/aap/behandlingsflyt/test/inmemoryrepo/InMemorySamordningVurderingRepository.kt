package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.util.concurrent.ConcurrentHashMap

object InMemorySamordningVurderingRepository : SamordningVurderingRepository {
    private val vurderinger = ConcurrentHashMap<BehandlingId, SamordningVurderingGrunnlag>()
    private val lock = Object()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningVurderingGrunnlag? {
        synchronized(lock) { return vurderinger[behandlingId] }
    }

    override fun hentHistoriskeVurderinger(
        sakId: SakId,
        behandlingId: BehandlingId
    ): List<SamordningVurderingGrunnlag> {
        synchronized(lock) {
            return emptyList()
        }
    }

    override fun lagreVurderinger(behandlingId: BehandlingId, samordningVurderinger: SamordningVurderingGrunnlag) {
        synchronized(lock) { vurderinger[behandlingId] = samordningVurderinger }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
    }
}