package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface StønadsperiodeRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vurderinger: Set<StønadsperiodeVurdering>)
    fun hentHvisEksisterer(behandlingId: BehandlingId): StønadsperiodeGrunnlag?
    fun tilbakestillGrunnlag(behandlingId: BehandlingId, forrigeBehandling: BehandlingId?)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    override fun slett(behandlingId: BehandlingId)
}
