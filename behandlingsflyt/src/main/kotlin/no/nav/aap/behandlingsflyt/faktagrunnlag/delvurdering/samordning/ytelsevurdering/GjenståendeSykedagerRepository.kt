package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.GjenståendeSykedager
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface GjenståendeSykedagerRepository : Repository {
    /**
     * Henter aktivt oppslag av gjenstående sykedager fra register.
     */
    fun hentHvisEksisterer(behandlingId: BehandlingId): GjenståendeSykedager?

    /**
     * Lagrer oppslaget. Deaktiverer eksisterende oppslag hvis det er ulikt.
     */
    fun lagre(behandlingId: BehandlingId, gjenståendeSykedager: GjenståendeSykedager)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}
