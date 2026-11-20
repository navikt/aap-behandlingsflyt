package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsBehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsoppgaveGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryOppfølgingsBehandlingRepository : OppfølgingsBehandlingRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, List<OppfølgingsoppgaveGrunnlag>>()

    override fun lagre(behandlingId: BehandlingId, grunnlag: OppfølgingsoppgaveGrunnlag) {
        this.grunnlag.compute(behandlingId) { _, tidligere -> tidligere.orEmpty() + grunnlag }
    }

    override fun hent(behandlingId: BehandlingId): OppfølgingsoppgaveGrunnlag? =
        grunnlag[behandlingId]?.single()

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Oppfølgingsbehandlinger kopierer ikke grunnlag.
    }

    override fun slett(behandlingId: BehandlingId) {
        grunnlag.remove(behandlingId)
    }
}
