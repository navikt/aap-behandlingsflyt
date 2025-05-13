package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface PåklagetBehandlingRepository: Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): PåklagetBehandlingGrunnlag?
    fun lagre(behandlingId: BehandlingId, formkravVurdering: PåklagetBehandlingVurdering)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}