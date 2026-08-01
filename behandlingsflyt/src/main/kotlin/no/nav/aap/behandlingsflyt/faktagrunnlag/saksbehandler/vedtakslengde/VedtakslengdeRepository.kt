package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.vedtakslengde.VedtakslengdeGrunnlag
import no.nav.aap.vedtakslengde.VedtakslengdeVurdering

interface VedtakslengdeRepository: Repository {
    fun lagre(behandlingId: BehandlingId, vurderinger: List<VedtakslengdeVurdering>)
    fun hentHvisEksisterer(behandlingId: BehandlingId): VedtakslengdeGrunnlag?
    fun deaktiverGrunnlag(behandlingId: BehandlingId)
}