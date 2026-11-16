package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdatoGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdatoGrunnlagId
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdatoRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InMemoryArenaMaksdatoRepository : ArenaMaksdatoRepository {
    private val store = ConcurrentHashMap<BehandlingId, ArenaMaksdatoGrunnlag>()
    private val alle = ConcurrentHashMap<ArenaMaksdatoGrunnlagId, ArenaMaksdatoGrunnlag>()
    private val nesteId = AtomicLong(1)

    fun reset() {
        store.clear()
        alle.clear()
    }

    override fun lagre(behandlingId: BehandlingId, arenaMaksdato: ArenaMaksdato?) {
        val eksisterende = store[behandlingId]
        if (eksisterende != null && eksisterende.arenaMaksdato == arenaMaksdato) return
        nyttGrunnlag(behandlingId, arenaMaksdato)
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): ArenaMaksdatoGrunnlag? =
        store[behandlingId]

    override fun hent(id: ArenaMaksdatoGrunnlagId): ArenaMaksdatoGrunnlag = alle.getValue(id)

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        store[fraBehandling]?.let { nyttGrunnlag(tilBehandling, it.arenaMaksdato) }
    }

    override fun slett(behandlingId: BehandlingId) {
        store.remove(behandlingId)
    }

    private fun nyttGrunnlag(behandlingId: BehandlingId, arenaMaksdato: ArenaMaksdato?) {
        val grunnlag = ArenaMaksdatoGrunnlag(ArenaMaksdatoGrunnlagId(nesteId.getAndIncrement()), arenaMaksdato)
        alle[grunnlag.id] = grunnlag
        store[behandlingId] = grunnlag
    }
}
