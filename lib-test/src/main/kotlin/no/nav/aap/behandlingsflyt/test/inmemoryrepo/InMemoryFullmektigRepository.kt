package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryFullmektigRepository : FullmektigRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, FullmektigGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): FullmektigGrunnlag? = grunnlag[behandlingId]

    override fun lagre(behandlingId: BehandlingId, vurdering: FullmektigVurdering) {
        grunnlag[behandlingId] = FullmektigGrunnlag(vurdering)
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Fullmektig vurderes på nytt for hver klagebehandling.
    }

    override fun slett(behandlingId: BehandlingId) {
        grunnlag.remove(behandlingId)
    }
}
