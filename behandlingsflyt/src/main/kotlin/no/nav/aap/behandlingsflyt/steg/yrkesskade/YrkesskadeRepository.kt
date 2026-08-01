package no.nav.aap.behandlingsflyt.steg.yrkesskade

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.yrkesskade.YrkesskadeGrunnlag
import no.nav.aap.yrkesskade.Yrkesskader

interface YrkesskadeRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): YrkesskadeGrunnlag?
    fun lagre(behandlingId: BehandlingId, registerYrkesskader: Yrkesskader?, oppgittYrkesskadeISøknad: Boolean?)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}