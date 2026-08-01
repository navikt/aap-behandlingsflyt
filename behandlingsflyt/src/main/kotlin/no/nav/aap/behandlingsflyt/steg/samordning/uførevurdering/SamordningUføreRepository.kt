package no.nav.aap.behandlingsflyt.steg.samordning.uførevurdering

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.samordning.SamordningUføreGrunnlag
import no.nav.aap.samordning.SamordningUføreVurdering

interface SamordningUføreRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningUføreGrunnlag?
    fun lagre(behandlingId: BehandlingId, vurdering: SamordningUføreVurdering)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}