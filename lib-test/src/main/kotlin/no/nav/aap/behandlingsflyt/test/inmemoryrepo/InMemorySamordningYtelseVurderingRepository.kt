package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InMemorySamordningYtelseVurderingRepository : SamordningYtelseVurderingRepository {
    private val ytelser = ConcurrentHashMap<BehandlingId, List<SamordningYtelse>>()
    private val vurderinger = ConcurrentHashMap<BehandlingId, List<SamordningVurdering>>()
    private val id = AtomicLong(0)

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningYtelseVurderingGrunnlag? {
        val vurderinger = vurderinger[behandlingId] ?: emptyList()
        return SamordningYtelseVurderingGrunnlag(
            ytelseGrunnlag = SamordningYtelseGrunnlag(ytelseId = 2, ytelser[behandlingId] ?: emptyList()),
            vurderingGrunnlag = SamordningVurderingGrunnlag(vurderingerId = 1, vurderinger = vurderinger)
        )
    }

    override fun lagreVurderinger(behandlingId: BehandlingId, samordningVurderinger: List<SamordningVurdering>) {
        vurderinger[behandlingId] = samordningVurderinger
    }

    override fun lagreYtelser(behandlingId: BehandlingId, samordningYtelser: List<SamordningYtelse>) {
        ytelser[behandlingId] = samordningYtelser
    }

    override fun hentSamordningYtelser(behandlingId: BehandlingId, ytelseId: Long): SamordningYtelseGrunnlag {
        return SamordningYtelseGrunnlag(
            ytelseId = 2,
            ytelser = ytelser[behandlingId] ?: emptyList()
        )
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        TODO("Not yet implemented")
    }
}