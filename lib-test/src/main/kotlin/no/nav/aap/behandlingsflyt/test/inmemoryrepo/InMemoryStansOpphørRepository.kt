package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.steg.rettighetstype.StansOpphørGrunnlag
import no.nav.aap.behandlingsflyt.steg.rettighetstype.StansOpphørRepository
import no.nav.aap.behandling.BehandlingId

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