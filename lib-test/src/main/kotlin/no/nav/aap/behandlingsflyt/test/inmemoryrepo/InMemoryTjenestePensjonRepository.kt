package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonForhold
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryTjenestePensjonRepository : TjenestePensjonRepository {
    private val tjenestePensjon = mutableMapOf<BehandlingId, List<TjenestePensjonForhold>>()
    private val lock = Object()
    override fun hentHvisEksisterer(behandlingId: BehandlingId): List<TjenestePensjonForhold>? {
        synchronized(lock) {
            return tjenestePensjon[behandlingId]
        }
    }

    override fun hent(behandlingId: BehandlingId): List<TjenestePensjonForhold> {
        synchronized(lock) {
            return tjenestePensjon[behandlingId]
                ?: throw IllegalStateException("Fant ikke tjeneste pensjon for behandlingId $behandlingId")
        }
    }

    override fun lagre(behandlingId: BehandlingId, tjenestePensjon: List<TjenestePensjonForhold>) {
        synchronized(lock) {
            this.tjenestePensjon[behandlingId] = tjenestePensjon
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        synchronized(lock) {
            tjenestePensjon[tilBehandling] = tjenestePensjon[fraBehandling].orEmpty()
        }
    }

    override fun slett(behandlingId: BehandlingId) {
    }


}