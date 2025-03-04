package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningerMedBegrunnelse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InMemorySamordningYtelseVurderingRepository : SamordningYtelseVurderingRepository {
    private val ytelser = ConcurrentHashMap<BehandlingId, List<SamordningYtelse>>()
    private val vurderinger = ConcurrentHashMap<BehandlingId, SamordningerMedBegrunnelse>()
    private val id = AtomicLong(0)

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningYtelseVurderingGrunnlag? {
        val samordningerMedBegrunnelse = vurderinger[behandlingId] ?: SamordningerMedBegrunnelse("", emptyList())
        return SamordningYtelseVurderingGrunnlag(
            ytelseGrunnlag = SamordningYtelseGrunnlag(ytelseId = 2, ytelser[behandlingId] ?: emptyList()),
            vurderingGrunnlag = SamordningVurderingGrunnlag(vurderingerId = 1, vurderinger = samordningerMedBegrunnelse.vurdering)
        )
    }

    override fun lagreVurderinger(behandlingId: BehandlingId, samordningVurderinger: SamordningerMedBegrunnelse) {
        vurderinger[behandlingId] = samordningVurderinger
    }

    override fun lagreYtelser(behandlingId: BehandlingId, samordningYtelser: List<SamordningYtelse>) {
        ytelser[behandlingId] = samordningYtelser
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        TODO("Not yet implemented")
    }
}