package no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

@JvmInline
value class ArenaMaksdatoGrunnlagId(val id: Long)

data class ArenaMaksdatoGrunnlag(val id: ArenaMaksdatoGrunnlagId, val arenaMaksdato: ArenaMaksdato?)

interface ArenaMaksdatoRepository : Repository {
    fun lagre(behandlingId: BehandlingId, arenaMaksdato: ArenaMaksdato?)
    fun hentHvisEksisterer(behandlingId: BehandlingId): ArenaMaksdatoGrunnlag?

    /** Henter grunnlaget med gitt id, også om det ikke lenger er aktivt. */
    fun hent(id: ArenaMaksdatoGrunnlagId): ArenaMaksdatoGrunnlag
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    override fun slett(behandlingId: BehandlingId)
}
