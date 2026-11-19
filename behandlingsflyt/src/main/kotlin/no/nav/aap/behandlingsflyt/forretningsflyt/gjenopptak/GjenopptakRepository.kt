package no.nav.aap.behandlingsflyt.forretningsflyt.gjenopptak

import no.nav.aap.behandlingsflyt.sakogbehandling.SakOgBehandling
import no.nav.aap.komponenter.repository.Repository

interface GjenopptakRepository : Repository {
    fun finnBehandlingerForGjennopptak(): List<SakOgBehandling>
}

