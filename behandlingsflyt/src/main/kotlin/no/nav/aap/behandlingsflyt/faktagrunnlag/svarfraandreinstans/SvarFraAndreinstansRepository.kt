package no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface SvarFraAndreinstansRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): SvarFraAndreinstansGrunnlag?
    fun lagre(behandlingId: BehandlingId, svarFraAndreinstansVurdering: SvarFraAndreinstansVurdering)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {}
}