package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.misc.uføre.UføreSøknad
import no.nav.aap.misc.uføre.UføreSøknadGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreSøknadRepository
import no.nav.aap.behandling.BehandlingId

object InMemoryUføreSøknadRepository : UføreSøknadRepository {
    private val mutex = Any()
    private val grunnlag = HashMap<BehandlingId, UføreSøknadGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId) = synchronized(mutex) {
        grunnlag[behandlingId]
    }

    override fun lagre(
        behandlingId: BehandlingId,
        uføreSøknad: UføreSøknad
    ) = synchronized(mutex) {
        grunnlag[behandlingId] = UføreSøknadGrunnlag(behandlingId, uføreSøknad)
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) = synchronized(mutex) {
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(mutex) {
            grunnlag.remove(behandlingId)
        }
    }
}