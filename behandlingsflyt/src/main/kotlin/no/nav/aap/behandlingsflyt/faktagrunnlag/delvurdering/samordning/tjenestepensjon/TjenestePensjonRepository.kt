package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface TjenestePensjonRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): List<SamhandlerForholdDto>?
    fun hent(behandlingId: BehandlingId): List<SamhandlerForholdDto>
    fun lagre(
        behandlingId: BehandlingId,
        tjenestePensjon: List<SamhandlerForholdDto>
    )

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}