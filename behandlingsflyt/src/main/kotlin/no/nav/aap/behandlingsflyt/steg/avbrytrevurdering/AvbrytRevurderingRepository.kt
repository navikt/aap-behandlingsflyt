package no.nav.aap.behandlingsflyt.steg.avbrytrevurdering

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface AvbrytRevurderingRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vurdering: AvbrytRevurderingVurdering)
    fun hentHvisEksisterer(behandlingId: BehandlingId): AvbrytRevurderingGrunnlag?
}