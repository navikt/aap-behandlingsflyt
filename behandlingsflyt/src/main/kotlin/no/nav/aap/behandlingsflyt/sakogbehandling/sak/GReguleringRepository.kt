package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.lookup.repository.Repository
import java.time.Year

interface GReguleringRepository : Repository {
    fun harGReguleringForÅr(sakId: SakId, år: Year): Boolean
    fun registrerGRegulering(sakId: SakId, år: Year)
}
