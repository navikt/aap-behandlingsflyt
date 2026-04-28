package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryArbeidsopptrappingRepository : ArbeidsopptrappingRepository {
    private val store = ConcurrentHashMap<BehandlingId, ArbeidsopptrappingGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): ArbeidsopptrappingGrunnlag? {
        return store[behandlingId]
    }

    override fun lagre(
        behandlingId: BehandlingId,
        arbeidsopptrappingVurderinger: List<ArbeidsopptrappingVurdering>
    ) {
        store[behandlingId] = ArbeidsopptrappingGrunnlag(
            vurderinger = arbeidsopptrappingVurderinger
        )
    }

    override fun slett(behandlingId: BehandlingId) {
        store.remove(behandlingId)
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        store[fraBehandling]?.let {  store[tilBehandling] = it }
    }
}