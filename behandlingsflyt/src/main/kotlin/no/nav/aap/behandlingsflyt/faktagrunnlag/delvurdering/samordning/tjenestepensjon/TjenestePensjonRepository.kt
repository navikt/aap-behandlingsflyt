package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface TjenestePensjonRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): TjenestePensjon?
    fun hent(behandlingId: BehandlingId): TjenestePensjon
    fun lagre(
        behandlingId: BehandlingId,
        tjenestePensjon: TjenestePensjon
    )

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}