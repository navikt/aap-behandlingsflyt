package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.util.concurrent.ConcurrentHashMap

object InMemoryRefusjonKravRepository : RefusjonkravRepository {
        private val vurderinger = ConcurrentHashMap<BehandlingId, List<RefusjonkravVurdering>>()

        override fun hentHvisEksisterer(behandlingId: BehandlingId): List<RefusjonkravVurdering>? =
            vurderinger[behandlingId]

        override fun hentHistoriskeVurderinger(
            sakId: SakId,
            behandlingId: BehandlingId,
        ): List<RefusjonkravVurdering> = emptyList()

        override fun lagre(
            sakId: SakId,
            behandlingId: BehandlingId,
            refusjonkravVurderinger: List<RefusjonkravVurdering>,
        ) {
            vurderinger[behandlingId] = refusjonkravVurderinger
        }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        vurderinger[fraBehandling]?.let { vurderinger[tilBehandling] = it }
    }

    override fun slett(behandlingId: BehandlingId) {
        vurderinger.remove(behandlingId)
    }
}