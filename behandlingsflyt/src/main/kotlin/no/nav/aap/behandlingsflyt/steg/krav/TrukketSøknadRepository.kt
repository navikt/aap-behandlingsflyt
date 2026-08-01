package no.nav.aap.behandlingsflyt.steg.krav

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface TrukketSøknadRepository: Repository {
    fun lagreTrukketSøknadVurdering(behandlingId: BehandlingId, vurdering: TrukketSøknadVurdering)
    fun hentTrukketSøknadVurderinger(behandlingId: BehandlingId): List<TrukketSøknadVurdering>

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}