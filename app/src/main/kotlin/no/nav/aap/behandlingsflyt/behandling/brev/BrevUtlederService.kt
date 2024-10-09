package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class BrevUtlederService {
    fun utledBrevbehov(behandlingId: BehandlingId): BrevBehov {
        return BrevBehov(null)
    }
}