package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.kontrakt.sak.Status

interface SakFlytRepository {

    fun oppdaterSakStatus(sakId: SakId, status: Status)

}

