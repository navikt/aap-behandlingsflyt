package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

object InMemoryMeldeperiodeRepository : MeldeperiodeRepository {
    private val meldeperioder = HashMap<BehandlingId, List<Periode>>()

    override fun hentFastsattDag(behandlingId: BehandlingId): LocalDate? = synchronized(this) {
        meldeperioder[behandlingId]?.firstOrNull()?.fom
    }

    override fun lagreFastsattDag(
        behandlingId: BehandlingId,
        fastsattDag: LocalDate,
    ) = synchronized(this) {
        this.meldeperioder[behandlingId] = listOf(
            Periode(fastsattDag, fastsattDag.plusDays(13))
        )
    }

    override fun slett(behandlingId: BehandlingId) {
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) = synchronized(this) {
        meldeperioder[tilBehandling] = meldeperioder[fraBehandling] ?: return@synchronized
    }
}