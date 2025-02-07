package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface SykdomRepository : Repository {
    fun lagre(
        behandlingId: BehandlingId,
        sykdomsvurderinger: List<Sykdomsvurdering>,
    )

    fun lagre(
        behandlingId: BehandlingId,
        sykdomsvurdering: Sykdomsvurdering,
    ) {
        lagre(behandlingId, listOf(sykdomsvurdering))
    }

    fun lagre(
        behandlingId: BehandlingId,
        yrkesskadevurdering: Yrkesskadevurdering?
    )

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hentHvisEksisterer(behandlingId: BehandlingId): SykdomGrunnlag?
    fun hent(behandlingId: BehandlingId): SykdomGrunnlag
}