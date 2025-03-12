package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface SamordningYtelseVurderingRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningVurderingGrunnlag?
    fun lagreVurderinger(behandlingId: BehandlingId, samordningVurderinger: List<SamordningVurdering>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}

interface SamordningYtelseRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningYtelseGrunnlag?
    fun lagre(behandlingId: BehandlingId, samordningYtelser: List<SamordningYtelse>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}