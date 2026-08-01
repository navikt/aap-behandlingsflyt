package no.nav.aap.behandlingsflyt.steg.stønadsperiode

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.stønadsperiode.StønadsperiodeGrunnlag
import no.nav.aap.stønadsperiode.StønadsperiodeVurdering

interface StønadsperiodeRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vurderinger: Set<StønadsperiodeVurdering>)
    fun hentHvisEksisterer(behandlingId: BehandlingId): StønadsperiodeGrunnlag?
    fun tilbakestillGrunnlag(behandlingId: BehandlingId, forrigeBehandling: BehandlingId?)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    override fun slett(behandlingId: BehandlingId)
}
