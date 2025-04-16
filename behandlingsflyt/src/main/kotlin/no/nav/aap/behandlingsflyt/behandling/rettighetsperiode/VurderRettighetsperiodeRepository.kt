package no.nav.aap.behandlingsflyt.behandling.rettighetsperiode

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface VurderRettighetsperiodeRepository: Repository {
    fun lagreVurdering(behandlingId: BehandlingId, vurdering: RettighetsperiodeVurdering)
    fun hentVurderinger(behandlingId: BehandlingId): List<RettighetsperiodeVurdering>

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}