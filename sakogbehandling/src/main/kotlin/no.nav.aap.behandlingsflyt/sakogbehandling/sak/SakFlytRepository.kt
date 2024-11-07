package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.verdityper.sakogbehandling.SakId

interface SakFlytRepository {

    fun oppdaterSakStatus(sakId: SakId, status: Status)

}

