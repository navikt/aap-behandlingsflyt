package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryVilkårsresultatRepository : VilkårsresultatRepository {

    private val memory = HashMap<BehandlingId, Vilkårsresultat>()
    private val lock = Object()

    override fun lagre(
        behandlingId: BehandlingId,
        vilkårsresultat: Vilkårsresultat
    ) {
        synchronized(lock) {
            memory.put(behandlingId, vilkårsresultat)
        }
    }

    override fun hent(behandlingId: BehandlingId): Vilkårsresultat {
        return synchronized(lock) {
            memory[behandlingId] ?: Vilkårsresultat()
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        synchronized(lock) {
            memory.put(tilBehandling, memory.getValue(fraBehandling))
        }
    }

    override fun slett(behandlingId: BehandlingId) {
    }
}