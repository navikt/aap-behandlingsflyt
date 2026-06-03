package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface KravRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vurderinger: Set<KravVurdering>)
    fun hent(behandlingId: BehandlingId): KravGrunnlag
    fun hentHvisEksisterer(behandlingId: BehandlingId): KravGrunnlag?
    override fun slett(behandlingId: BehandlingId)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}


