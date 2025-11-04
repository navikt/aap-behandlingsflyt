package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface SamordningVurderingRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningVurderingGrunnlag?
    fun hentHistoriskeVurderinger(sakId: SakId, behandlingId: BehandlingId): List<SamordningVurderingGrunnlag>
    fun lagreVurderinger(
        behandlingId: BehandlingId,
        samordningVurderinger: SamordningVurderingGrunnlag
    )

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}
