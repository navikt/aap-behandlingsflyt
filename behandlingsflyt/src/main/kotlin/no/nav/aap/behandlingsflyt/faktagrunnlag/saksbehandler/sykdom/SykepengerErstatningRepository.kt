package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface SykepengerErstatningRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vurderinger: List<SykepengerVurdering>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hentHvisEksisterer(behandlingId: BehandlingId): SykepengerErstatningGrunnlag?
}