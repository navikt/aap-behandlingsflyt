package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.meldeperiode.MeldeperiodeRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode

object InMemoryMeldeperiodeRepository: MeldeperiodeRepository {
    private val meldeperioder = HashMap<BehandlingId, List<Periode>>()

    override fun hent(behandlingId: BehandlingId) = synchronized(this) {
        meldeperioder[behandlingId].orEmpty()
    }

    override fun lagre(behandlingId: BehandlingId, meldeperioder: List<Periode>) = synchronized(this) {
        this.meldeperioder[behandlingId] = meldeperioder
    }

    override fun slett(behandlingId: BehandlingId) {
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) = synchronized(this) {
        meldeperioder[tilBehandling] = meldeperioder[fraBehandling] ?: return@synchronized
    }
}