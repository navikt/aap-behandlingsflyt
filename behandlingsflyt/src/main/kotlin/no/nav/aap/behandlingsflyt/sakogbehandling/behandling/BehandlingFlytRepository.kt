package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.repository.Repository

interface BehandlingFlytRepository : Repository {

    fun oppdaterBehandlingStatus(behandlingId: BehandlingId, status: Status)
    fun loggBesøktSteg(behandlingId: BehandlingId, tilstand: StegTilstand)
}
