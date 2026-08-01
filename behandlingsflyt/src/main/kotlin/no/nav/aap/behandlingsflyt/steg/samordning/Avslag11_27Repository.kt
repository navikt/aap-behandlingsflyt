package no.nav.aap.behandlingsflyt.steg.samordning

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.samordning.avslag11_27.Avslag11_27Grunnlag
import no.nav.aap.samordning.avslag11_27.Avslag11_27Vurdering

@Suppress("ClassName")
interface Avslag11_27Repository : Repository {
    fun lagre(behandlingId: BehandlingId, vurderinger: Set<Avslag11_27Vurdering>)
    fun hentHvisEksisterer(behandlingId: BehandlingId): Avslag11_27Grunnlag?
    fun tilbakestillGrunnlag(behandlingId: BehandlingId, forrigeBehandling: BehandlingId?)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    override fun slett(behandlingId: BehandlingId)
}