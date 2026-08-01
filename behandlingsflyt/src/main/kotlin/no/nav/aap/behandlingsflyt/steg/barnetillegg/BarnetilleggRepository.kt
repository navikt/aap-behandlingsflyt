package no.nav.aap.behandlingsflyt.steg.barnetillegg

import no.nav.aap.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface BarnetilleggRepository : Repository {
    fun hentHvisEksisterer(behandlingsId: BehandlingId): BarnetilleggGrunnlag?
    fun lagre(behandlingId: BehandlingId, barnetilleggPerioder: List<BarnetilleggPeriode>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}