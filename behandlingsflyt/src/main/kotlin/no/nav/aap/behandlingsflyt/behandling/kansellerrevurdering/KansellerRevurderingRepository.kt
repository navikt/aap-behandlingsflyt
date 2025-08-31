package no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface KansellerRevurderingRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vurdering: KansellerRevurderingVurdering)
    fun hentHvisEksisterer(behandlingId: BehandlingId): KansellerRevurderingGrunnlag?
}