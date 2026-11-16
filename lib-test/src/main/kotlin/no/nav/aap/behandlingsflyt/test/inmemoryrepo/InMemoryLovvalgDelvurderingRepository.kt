package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.LovvalgDelvurderingerGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.LovvalgDelvurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryLovvalgDelvurderingRepository : LovvalgDelvurderingRepository {
    private val grunnlag = mutableMapOf<BehandlingId, List<Long>>()
    private val vurderinger = mutableMapOf<Long, List<LovvalgDelvurderingerGrunnlag>>()
    private var nesteId = 0L
    private val lock = Any()

    override fun hentAlle(behandlingId: BehandlingId): List<LovvalgDelvurderingerGrunnlag> =
        synchronized(lock) {
            grunnlag[behandlingId]?.lastOrNull()?.let { vurderinger.getValue(it) }.orEmpty()
        }

    override fun lagre(behandlingId: BehandlingId, delvurderinger: List<LovvalgDelvurderingerGrunnlag>) {
        require(delvurderinger.isNotEmpty()) { "Må lagre minst én delvurdering" }
        require(delvurderinger.map { it.relevantDato }.distinct().size == delvurderinger.size) {
            "Kun én delvurdering per relevant dato"
        }
        synchronized(lock) {
            if (hentAlle(behandlingId).toSet() == delvurderinger.toSet()) return

            val id = ++nesteId
            vurderinger[id] = delvurderinger.sortedBy { it.relevantDato }
            grunnlag[behandlingId] = grunnlag[behandlingId].orEmpty() + id
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            val slettedeIder = grunnlag.remove(behandlingId).orEmpty()
            val refererteIder = grunnlag.values.flatten().toSet()
            slettedeIder.filterNot { it in refererteIder }.forEach { vurderinger.remove(it) }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        synchronized(lock) {
            grunnlag[fraBehandling]?.lastOrNull()?.let {
                require(tilBehandling !in grunnlag) { "Målbehandlingen har allerede et grunnlag" }
                grunnlag[tilBehandling] = listOf(it)
            }
        }
    }
}
