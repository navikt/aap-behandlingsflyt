package no.nav.aap.behandlingsflyt.underveis

import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.Period

class KvoteService {

    private val ANTALL_ARBEIDSDAGER_I_ÅRET = 260

    fun beregn(behandlingId: BehandlingId): Kvote {
        return Kvote(Period.ofDays(ANTALL_ARBEIDSDAGER_I_ÅRET * 3))
    }
}