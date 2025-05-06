package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface SamordningAndreStatligeYtelserRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningAndreStatligeYtelserGrunnlag?
    fun lagre(behandlingId: BehandlingId, vurdering: SamordningAndreStatligeYtelserVurdering)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}