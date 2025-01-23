package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface YrkesskadeRepository : Repository{
    fun hentHvisEksisterer(behandlingId: BehandlingId): YrkesskadeGrunnlag?
    fun lagre(behandlingId: BehandlingId, yrkesskader: Yrkesskader?)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}