package no.nav.aap.behandlingsflyt.flyt.testutil

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class InMemorySamordningYtelseVurderingRepository : SamordningYtelseVurderingRepository {
    val grunnlag = ConcurrentHashMap<BehandlingId, SamordningYtelseVurderingGrunnlag>()
    private val id = AtomicLong(0)

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningYtelseVurderingGrunnlag? {
        return grunnlag[behandlingId]
    }

    override fun lagreVurderinger(behandlingId: BehandlingId, samordningVurderinger: List<SamordningVurdering>) {
        TODO("Not yet implemented")
    }

    override fun lagreYtelser(behandlingId: BehandlingId, samordningYtelser: List<SamordningYtelse>) {
        TODO("Not yet implemented")
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        TODO("Not yet implemented")
    }
}