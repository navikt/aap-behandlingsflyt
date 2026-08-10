package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.migrering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface MigreringsdatoRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): MigreringsdatoGrunnlag?
    fun lagreVurdering(behandlingId: BehandlingId, vurdering: MigreringsdatoVurdering)
    fun deaktiverGrunnlag(behandlingId: BehandlingId)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    override fun slett(behandlingId: BehandlingId)
}
