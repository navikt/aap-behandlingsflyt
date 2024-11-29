package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class KvoteService {

    private val ANTALL_ARBEIDSDAGER_I_ÅRET = 260

    fun beregn(behandlingId: BehandlingId): Kvoter {
        return Kvoter.create(
            standardkvote = ANTALL_ARBEIDSDAGER_I_ÅRET * 3,
            studentkvote = ANTALL_ARBEIDSDAGER_I_ÅRET/2,
            sykepengeerstatningkvote = ANTALL_ARBEIDSDAGER_I_ÅRET/2
        )
    }
}