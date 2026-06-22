package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface TestAutomatiskMeldekortSakRepository : Repository {
    fun leggTil(sakId: SakId)
    fun eksisterer(sakId: SakId): Boolean
    fun hentAlle(): List<SakId>
}
