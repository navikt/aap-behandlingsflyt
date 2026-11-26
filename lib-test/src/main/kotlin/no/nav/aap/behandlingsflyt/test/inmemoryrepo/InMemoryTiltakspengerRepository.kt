package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.tiltakspenger.TiltakspengerRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryTiltakspengerRepository : TiltakspengerRepository {
    private val perioder = ConcurrentHashMap<BehandlingId, List<TiltakspengerPeriode>>()

    override fun hent(behandlingId: BehandlingId): List<TiltakspengerPeriode> = perioder[behandlingId].orEmpty()

    override fun lagre(behandlingId: BehandlingId, tiltakspengerPeriode: List<TiltakspengerPeriode>) {
        perioder[behandlingId] = tiltakspengerPeriode.toList()
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        perioder[fraBehandling]?.let { perioder[tilBehandling] = it }
    }

    override fun slett(behandlingId: BehandlingId) {
        perioder.remove(behandlingId)
    }
}
