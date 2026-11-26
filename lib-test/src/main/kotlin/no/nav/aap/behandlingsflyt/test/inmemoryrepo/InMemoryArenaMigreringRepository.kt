package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
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

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
    }

    override fun slett(behandlingId: BehandlingId) {
        val behandling = InMemoryBehandlingRepository.hentHvisEksisterer(behandlingId) ?: return
        if (behandling.typeBehandling() != TypeBehandling.Førstegangsbehandling) return
        store.remove(behandling.sakId)
    }

    fun reset() {
        store.clear()
    }
}
