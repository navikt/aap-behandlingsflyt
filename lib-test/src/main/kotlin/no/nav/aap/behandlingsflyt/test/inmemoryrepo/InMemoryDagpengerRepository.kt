package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dagpenger.DagpengerRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryDagpengerRepository : DagpengerRepository {
    private val perioder = ConcurrentHashMap<BehandlingId, List<DagpengerPeriode>>()

    override fun hent(behandlingId: BehandlingId): List<DagpengerPeriode> = perioder[behandlingId].orEmpty()

    override fun lagre(behandlingId: BehandlingId, dagpengerPeriode: List<DagpengerPeriode>) {
        perioder[behandlingId] = dagpengerPeriode.toList()
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        perioder[fraBehandling]?.let { perioder[tilBehandling] = it }
    }

    override fun slett(behandlingId: BehandlingId) {
        perioder.remove(behandlingId)
    }
}
