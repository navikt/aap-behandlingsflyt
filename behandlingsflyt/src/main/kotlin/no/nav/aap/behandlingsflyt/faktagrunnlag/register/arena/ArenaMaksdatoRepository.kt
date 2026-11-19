package no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

data class ArenaMaksdatoGrunnlag(val arenaMaksdato: ArenaMaksdato?)

interface ArenaMaksdatoRepository : Repository {
    fun lagre(behandlingId: BehandlingId, arenaMaksdato: ArenaMaksdato?)
    fun hentHvisEksisterer(behandlingId: BehandlingId): ArenaMaksdatoGrunnlag?
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    override fun slett(behandlingId: BehandlingId)
}
