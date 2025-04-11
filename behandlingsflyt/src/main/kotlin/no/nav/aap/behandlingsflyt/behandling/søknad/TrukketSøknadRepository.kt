package no.nav.aap.behandlingsflyt.behandling.søknad

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface TrukketSøknadRepository: Repository {
    fun lagreTrukketSøknadVurdering(behandlingId: BehandlingId, vurdering: TrukketSøknadVurdering)
    fun hentTrukketSøknadVurderinger(behandlingId: BehandlingId): List<TrukketSøknadVurdering>

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}