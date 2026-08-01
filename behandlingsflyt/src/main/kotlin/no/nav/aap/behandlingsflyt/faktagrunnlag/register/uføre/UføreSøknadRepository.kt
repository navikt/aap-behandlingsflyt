package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.misc.uføre.UføreSøknad
import no.nav.aap.misc.uføre.UføreSøknadGrunnlag

interface UføreSøknadRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): UføreSøknadGrunnlag?
    fun lagre(behandlingId: BehandlingId, uføreSøknad: UføreSøknad)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}