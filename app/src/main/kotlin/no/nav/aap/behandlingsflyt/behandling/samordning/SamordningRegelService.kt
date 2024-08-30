package no.nav.aap.behandlingsflyt.behandling.samordning

import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class SamordningRegelService {
    // Alt denne trenger å gjøre er å slå sammen alle ytelsesgraderinger til en tidslinje
    fun vurder(behandlingId: BehandlingId): Tidslinje<SamordningGradering> {
        return Tidslinje()
    }
}