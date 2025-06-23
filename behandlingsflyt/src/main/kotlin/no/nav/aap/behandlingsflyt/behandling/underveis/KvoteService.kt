package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

class KvoteService {

    private val ANTALL_ARBEIDSDAGER_I_ÅRET = 260

    fun beregn(behandlingId: BehandlingId): Kvoter {
        // TODO ta hensyn til når du har rett på hvilken kvote (Kvoter-objektet burde ha en tidslinje et sted)
        // Dette burde skje ved å hente en tidslinje av rettighetstyper
        return Kvoter.create(
            /* Så lenge Arena har 784 må vi ha samme som dem, i stede for ANTALL_ARBEIDSDAGER_I_ÅRET * 3. */
            ordinærkvote = 784,
            studentkvote = ANTALL_ARBEIDSDAGER_I_ÅRET / 2,
            sykepengeerstatningkvote = ANTALL_ARBEIDSDAGER_I_ÅRET / 2
        )
    }
}