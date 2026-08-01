package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.steg.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandling.BehandlingId

object InMemoryBarnetilleggRepository : BarnetilleggRepository {

    private val memory = HashMap<BehandlingId, BarnetilleggGrunnlag>()
    private val lock = Any()

    override fun hentHvisEksisterer(behandlingsId: BehandlingId): BarnetilleggGrunnlag? =
        synchronized(lock) { memory[behandlingsId] }

    override fun lagre(behandlingId: BehandlingId, barnetilleggPerioder: List<BarnetilleggPeriode>) =
        synchronized(lock) { memory[behandlingId] = BarnetilleggGrunnlag(barnetilleggPerioder) }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId): Unit =
        synchronized(lock) {
            memory[fraBehandling]?.let { memory[tilBehandling] = it }
        }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            memory.remove(behandlingId)
        }
    }
}