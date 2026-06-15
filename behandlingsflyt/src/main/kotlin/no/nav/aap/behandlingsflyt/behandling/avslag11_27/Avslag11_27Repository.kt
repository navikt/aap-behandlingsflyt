package no.nav.aap.behandlingsflyt.behandling.avslag11_27

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface Avslag11_27Repository : Repository {
    fun lagre(behandlingId: BehandlingId, vurderinger: List<Avslag11_27Vurdering>)
    fun hentHvisEksisterer(behandlingId: BehandlingId): Avslag11_27Grunnlag?
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    override fun slett(behandlingId: BehandlingId)
}