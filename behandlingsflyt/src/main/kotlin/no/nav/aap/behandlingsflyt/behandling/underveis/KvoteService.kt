package no.nav.aap.behandlingsflyt.behandling.underveis

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

class KvoteService {

    private val ANTALL_ARBEIDSDAGER_I_ÅRET = 260

    fun beregn(behandlingId: BehandlingId): Kvoter {
        return Kvoter.create(
            ordinærkvote = ANTALL_ARBEIDSDAGER_I_ÅRET * 3,
            studentkvote = ANTALL_ARBEIDSDAGER_I_ÅRET/2,
            sykepengeerstatningkvote = ANTALL_ARBEIDSDAGER_I_ÅRET/2
        )
    }
}