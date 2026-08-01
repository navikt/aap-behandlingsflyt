package no.nav.aap.behandlingsflyt.steg.rettighetsperiode

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.krav.RettighetsperiodeVurdering
import no.nav.aap.lookup.repository.Repository

interface VurderRettighetsperiodeRepository: Repository {
    fun lagreVurdering(behandlingId: BehandlingId, vurdering: RettighetsperiodeVurdering?)
    fun hentVurdering(behandlingId: BehandlingId): RettighetsperiodeVurdering?

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}