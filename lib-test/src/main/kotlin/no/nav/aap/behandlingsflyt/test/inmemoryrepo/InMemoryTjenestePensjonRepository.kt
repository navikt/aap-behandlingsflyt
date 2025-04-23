package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.SamhandlerForholdDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryTjenestePensjonRepository : TjenestePensjonRepository {
    private val tjenestePensjon = mutableMapOf<BehandlingId, List<SamhandlerForholdDto>>()
    private val lock = Object()
    override fun hentHvisEksisterer(behandlingId: BehandlingId): List<SamhandlerForholdDto>? {
        synchronized(lock) {
            return tjenestePensjon[behandlingId]
        }
    }

    override fun hent(behandlingId: BehandlingId): List<SamhandlerForholdDto> {
        synchronized(lock) {
            return tjenestePensjon[behandlingId]
                ?: throw IllegalStateException("Fant ikke tjeneste pensjon for behandlingId $behandlingId")
        }
    }

    override fun lagre(behandlingId: BehandlingId, tjenestePensjon: List<SamhandlerForholdDto>) {
        synchronized(lock) {
            this.tjenestePensjon[behandlingId] = tjenestePensjon
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        synchronized(lock) {
            tjenestePensjon[tilBehandling] = tjenestePensjon[fraBehandling]?: emptyList()
        }
    }


}