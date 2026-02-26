package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryStansOpphørRepository: StansOpphørRepository {

    private val memory = HashMap<BehandlingId, StansOpphørGrunnlag>()
    private val lock = Any()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): StansOpphørGrunnlag? {
        return synchronized(lock) {
            memory[behandlingId]
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        grunnlag: StansOpphørGrunnlag
    ) {
       synchronized(lock) {
           memory[behandlingId] = grunnlag
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
            synchronized(lock) {
                memory.remove(behandlingId)
            }
    }
}