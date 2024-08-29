package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class SamordningRegelService {
    fun vurder(behandlingId: BehandlingId): Tidslinje<SamordningGradering> {
        return Tidslinje()
    }
}