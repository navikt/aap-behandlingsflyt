package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykepengerOgFerieOppgittISøknad.SykepengerOgFerieOppgittISøknadRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykepengerOgFerieOppgittISøknad.SykepengerOgFerieSøknad
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemorySykepengerOgFerieOppgittISøknadRepository : SykepengerOgFerieOppgittISøknadRepository {
    private val store = ConcurrentHashMap<BehandlingId, SykepengerOgFerieSøknad>()

    override fun lagre(behandlingId: BehandlingId, sykepengerOgFerie: SykepengerOgFerieSøknad) {
        store[behandlingId] = sykepengerOgFerie
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        store[fraBehandling]?.let { store[tilBehandling] = it }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SykepengerOgFerieSøknad? {
        return store[behandlingId]
    }

    override fun slett(behandlingId: BehandlingId) {
        store.remove(behandlingId)
    }
}
