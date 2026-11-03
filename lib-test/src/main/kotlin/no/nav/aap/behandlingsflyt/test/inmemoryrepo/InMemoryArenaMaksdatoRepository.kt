package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdatoGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdatoRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryArenaMaksdatoRepository : ArenaMaksdatoRepository {
    private val store = ConcurrentHashMap<BehandlingId, ArenaMaksdatoGrunnlag>()

    fun reset() = store.clear()

    override fun lagre(behandlingId: BehandlingId, arenaMaksdato: ArenaMaksdato?) {
        store[behandlingId] = ArenaMaksdatoGrunnlag(arenaMaksdato)
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): ArenaMaksdatoGrunnlag? =
        store[behandlingId]

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        store[fraBehandling]?.let { store[tilBehandling] = it }
    }

    override fun slett(behandlingId: BehandlingId) {
        store.remove(behandlingId)
    }
}
