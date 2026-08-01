package no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface VilkårsresultatRepository : Repository{
    fun lagre(behandlingId: BehandlingId, vilkårsresultat: Vilkårsresultat)
    fun hent(behandlingId: BehandlingId): Vilkårsresultat
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}