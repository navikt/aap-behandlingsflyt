package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.hendelse.datadeling.ArenaSakMedVedtakResponse
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

    override fun lagreArenaSakData(sakId: SakId, data: ArenaSakMedVedtakResponse) {
        val eksisterende = requireNotNull(store[sakId]) {
            "Fant ingen arenamigrering å lagre arenasak-data på for sak $sakId"
        }
        store[sakId] = eksisterende.copy(arenaSakData = data)
    }

    fun reset() {
        store.clear()
    }
}
