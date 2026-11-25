package no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

/**
 * @param arenaMaksdato Null når Arena er spurt uten treff.
 * @param arenaMaksdatoId Raden i `arena_maksdato` som [arenaMaksdato] er lest fra.
 */
data class ArenaMaksdatoGrunnlag(val arenaMaksdato: ArenaMaksdato?, val arenaMaksdatoId: Long?) {
    init {
        require((arenaMaksdato == null) == (arenaMaksdatoId == null)) {
            "[Utviklerfeil] Arena-maksdato og id må enten begge være satt eller begge mangle"
        }
    }
}

interface ArenaMaksdatoRepository : Repository {
    fun lagre(behandlingId: BehandlingId, arenaMaksdato: ArenaMaksdato?)
    fun hentHvisEksisterer(behandlingId: BehandlingId): ArenaMaksdatoGrunnlag?
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    override fun slett(behandlingId: BehandlingId)
}
