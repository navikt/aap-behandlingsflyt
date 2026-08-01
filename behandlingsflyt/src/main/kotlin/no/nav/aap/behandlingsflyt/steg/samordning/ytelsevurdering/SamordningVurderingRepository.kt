package no.nav.aap.behandlingsflyt.steg.samordning.ytelsevurdering

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.misc.SamordningVurderingGrunnlag
import no.nav.aap.lookup.repository.Repository

interface SamordningVurderingRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningVurderingGrunnlag?
    fun lagreVurderinger(
        behandlingId: BehandlingId,
        samordningVurderinger: SamordningVurderingGrunnlag
    )
    fun deaktiverGrunnlag(behandlingId: BehandlingId)

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}
