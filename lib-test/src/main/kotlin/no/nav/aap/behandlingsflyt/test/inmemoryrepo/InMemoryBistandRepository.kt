package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryBistandRepository : BistandRepository {
    private val bistandsvurderingerMap = ConcurrentHashMap<BehandlingId, List<Bistandsvurdering>>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): BistandGrunnlag? {
        val vurderinger = bistandsvurderingerMap[behandlingId]
        return if (vurderinger != null) {
            BistandGrunnlag(vurderinger)
        } else {
            null
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        bistandsvurderinger: List<Bistandsvurdering>
    ) {
        bistandsvurderingerMap[behandlingId] = bistandsvurderinger
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        bistandsvurderingerMap[fraBehandling]?.let {
            bistandsvurderingerMap[tilBehandling] = it
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        bistandsvurderingerMap.remove(behandlingId)
    }
}