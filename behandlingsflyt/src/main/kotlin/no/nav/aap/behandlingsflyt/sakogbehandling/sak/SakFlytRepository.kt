package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.kontrakt.sak.Status
import no.nav.aap.repository.Repository

interface SakFlytRepository : Repository {

    fun oppdaterSakStatus(sakId: SakId, status: Status)

}

