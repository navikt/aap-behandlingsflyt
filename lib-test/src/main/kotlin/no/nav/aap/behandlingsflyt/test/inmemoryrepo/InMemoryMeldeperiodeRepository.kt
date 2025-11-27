package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeUtleder
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode

object InMemoryMeldeperiodeRepository: MeldeperiodeRepository {
    private val meldeperioder = HashMap<BehandlingId, List<Periode>>()

    override fun hentFørsteMeldeperiode(behandlingId: BehandlingId): Periode? = synchronized(this) {
        meldeperioder[behandlingId]?.firstOrNull()
    }

    override fun hentMeldeperioder(
        behandlingId: BehandlingId,
        periode: Periode
    ): List<Periode> = MeldeperiodeUtleder.utledMeldeperiode(hentFørsteMeldeperiode(behandlingId)?.fom, periode)

    override fun lagreFørsteMeldeperiode(
        behandlingId: BehandlingId,
        meldeperiode: Periode?
    ) = synchronized(this) {
        this.meldeperioder[behandlingId] = meldeperiode?.let { listOf(it) } ?: emptyList()
    }

    override fun slett(behandlingId: BehandlingId) {
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) = synchronized(this) {
        meldeperioder[tilBehandling] = meldeperioder[fraBehandling] ?: return@synchronized
    }
}