package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryBehandlendeEnhetRepository : BehandlendeEnhetRepository {
    private val grunnlag = mutableMapOf<BehandlingId, BehandlendeEnhetGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): BehandlendeEnhetGrunnlag? {
        return grunnlag[behandlingId]
    }

    override fun lagre(
        behandlingId: BehandlingId,
        behandlendeEnhetVurdering: BehandlendeEnhetVurdering
    ) {
        grunnlag[behandlingId] = BehandlendeEnhetGrunnlag(
            vurdering = behandlendeEnhetVurdering
        )
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
        TODO("Not yet implemented")
    }

}
