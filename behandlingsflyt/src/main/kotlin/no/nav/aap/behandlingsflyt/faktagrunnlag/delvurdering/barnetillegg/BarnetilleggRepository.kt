package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface BarnetilleggRepository : Repository {
    fun hentHvisEksisterer(behandlingsId: BehandlingId): BarnetilleggGrunnlag?
    fun lagre(behandlingId: BehandlingId, barnetilleggPerioder: List<BarnetilleggPeriode>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}