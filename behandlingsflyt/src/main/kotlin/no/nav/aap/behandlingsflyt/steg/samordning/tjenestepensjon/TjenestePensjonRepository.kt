package no.nav.aap.behandlingsflyt.steg.samordning.tjenestepensjon

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface TjenestePensjonRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): List<TjenestePensjonForhold>?
    fun hent(behandlingId: BehandlingId): List<TjenestePensjonForhold>
    fun lagre(
        behandlingId: BehandlingId,
        tjenestePensjon: List<TjenestePensjonForhold>
    )

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}