package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.lookup.repository.Repository

interface GReguleringRepository : Repository {
    fun harGReguleringForÅr(sakId: SakId, år: Int): Boolean
    fun registrerGRegulering(sakId: SakId, år: Int)
}
