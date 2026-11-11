package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsBehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling.OppfølgingsoppgaveGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryOppfølgingsBehandlingRepository : OppfølgingsBehandlingRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, OppfølgingsoppgaveGrunnlag>()

    override fun lagre(behandlingId: BehandlingId, grunnlag: OppfølgingsoppgaveGrunnlag) {
        this.grunnlag[behandlingId] = grunnlag.copy(opplysningerTilRevurdering = grunnlag.opplysningerTilRevurdering.toList())
    }

    override fun hent(behandlingId: BehandlingId): OppfølgingsoppgaveGrunnlag? = grunnlag[behandlingId]

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Oppfølgingsoppgaven gjelder bare behandlingen der den ble vurdert.
    }

    override fun slett(behandlingId: BehandlingId) {
        grunnlag.remove(behandlingId)
    }
}
