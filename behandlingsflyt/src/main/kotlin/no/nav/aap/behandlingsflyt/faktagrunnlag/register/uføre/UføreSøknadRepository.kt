package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface UføreSøknadRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): UføreSøknadGrunnlag?
    fun lagre(behandlingId: BehandlingId, uføreSøknad: UføreSøknad)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}