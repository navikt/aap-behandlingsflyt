package no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface AvbrytRevurderingRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vurdering: AvbrytRevurderingVurdering)
    fun hentHvisEksisterer(behandlingId: BehandlingId): AvbrytRevurderingGrunnlag?
}