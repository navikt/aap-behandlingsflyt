package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemorySamordningYtelseVurderingRepository : SamordningYtelseVurderingRepository {
    private val ytelser = ConcurrentHashMap<BehandlingId, List<SamordningYtelse>>()
    private val vurderinger = ConcurrentHashMap<BehandlingId, List<SamordningVurdering>>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningVurderingGrunnlag? {
        val vurderinger = vurderinger[behandlingId] ?: emptyList()
        return SamordningVurderingGrunnlag(vurderingerId = 1, vurderinger = vurderinger)

    }

    override fun lagreVurderinger(behandlingId: BehandlingId, samordningVurderinger: List<SamordningVurdering>) {
        vurderinger[behandlingId] = samordningVurderinger
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        TODO("Not yet implemented")
    }
}