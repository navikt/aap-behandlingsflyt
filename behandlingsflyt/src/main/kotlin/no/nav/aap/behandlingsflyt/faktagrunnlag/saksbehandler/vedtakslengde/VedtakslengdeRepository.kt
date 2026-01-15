package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface VedtakslengdeRepository: Repository {
    fun lagre(behandlingId: BehandlingId, vurdering: VedtakslengdeVurdering)
    fun hentHvisEksisterer(behandlingId: BehandlingId): VedtakslengdeGrunnlag?
    fun deaktiverGrunnlag(behandlingId: BehandlingId)
}