package no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface KansellerRevurderingRepository : Repository {
    fun lagreKansellerRevurderingVurdering(behandlingId: BehandlingId, vurdering: KansellerRevurderingVurdering)
    fun hentKansellertRevurderingGrunnlag(behandlingId: BehandlingId): KansellerRevurderingGrunnlag?
}