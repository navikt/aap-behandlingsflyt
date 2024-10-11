package no.nav.aap.behandlingsflyt.behandling.brev

import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class BrevUtlederService {
    fun utledBrevbehov(behandlingId: BehandlingId): BrevBehov {
        // TODO: Sjekk på behandlingen og utled hva som har skjedd for å avgjøre om det skal sendes et brev
        return BrevBehov(null)
    }
}