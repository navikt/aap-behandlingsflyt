package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.repository.Repository

interface VilkårsresultatRepository : Repository{
    fun lagre(behandlingId: BehandlingId, vilkårsresultat: Vilkårsresultat)
    fun hent(behandlingId: BehandlingId): Vilkårsresultat
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}