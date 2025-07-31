package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface SykdomsvurderingForBrevRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vurdering: SykdomsvurderingForBrev)
    fun hent(behandlingId: BehandlingId): SykdomsvurderingForBrev?
}