package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface SamordningYtelseVurderingRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningYtelseVurderingGrunnlag?
    fun lagreVurderinger(behandlingId: BehandlingId, samordningVurderinger: List<SamordningVurdering>)
    fun lagreYtelser(behandlingId: BehandlingId, samordningYtelser: List<SamordningYtelse>)
    fun hentSamordningYtelser(behandlingId: BehandlingId, ytelseId: Long) : SamordningYtelseGrunnlag
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}