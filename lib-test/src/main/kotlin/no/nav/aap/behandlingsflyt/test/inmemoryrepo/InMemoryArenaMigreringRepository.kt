package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.ArenaMigrering
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.ArenaMigreringRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId

object InMemoryArenaMigreringRepository : ArenaMigreringRepository {
    private val store = mutableMapOf<SakId, ArenaMigrering>()

    override fun lagre(migrering: ArenaMigrering) {
        store[migrering.sakId] = migrering
    }

    override fun hentForSakHvisEksisterer(sakId: SakId): ArenaMigrering? {
        return store[sakId]
    }

    fun reset() {
        store.clear()
    }
}
