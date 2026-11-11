package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.GjenståendeSykedager
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface GjenståendeSykedagerRepository : Repository {
    fun lagre(behandlingId: BehandlingId, gjenståendeSykedager: GjenståendeSykedager)
    fun hentHvisEksisterer(behandlingId: BehandlingId): GjenståendeSykedager?
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}
