package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemorySvarFraAndreinstansRepository : SvarFraAndreinstansRepository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, SvarFraAndreinstansGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SvarFraAndreinstansGrunnlag? = grunnlag[behandlingId]

    override fun lagre(behandlingId: BehandlingId, svarFraAndreinstansVurdering: SvarFraAndreinstansVurdering) {
        grunnlag[behandlingId] = SvarFraAndreinstansGrunnlag(
            svarFraAndreinstansVurdering.copy(vilkårSomOmgjøres = svarFraAndreinstansVurdering.vilkårSomOmgjøres.toList())
        )
    }

    override fun slett(behandlingId: BehandlingId) {
        // Postgres beholder også svaret fra andreinstans ved sletting av klagegrunnlag.
    }
}
