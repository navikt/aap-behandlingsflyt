package no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface UføreRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): UføreGrunnlag?
    fun hentEldsteGrunnlag(behandlingId: BehandlingId): UføreGrunnlag?
    fun lagre(behandlingId: BehandlingId, uføre: Set<Uføre>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}