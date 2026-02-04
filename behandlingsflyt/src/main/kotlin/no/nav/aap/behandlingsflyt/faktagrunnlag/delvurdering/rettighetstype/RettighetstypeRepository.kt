package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface RettighetstypeRepository : Repository {
    fun hent(behandlingId: BehandlingId): RettighetstypePerioder
    fun hentHvisEksisterer(behandlingId: BehandlingId): RettighetstypePerioder?
    fun lagre(
        behandlingId: BehandlingId,
        rettighetstypePerioder: Set<RettighetstypePeriode>,
        faktagrunnlag: RettighetstypeFaktagrunnlag,
        versjon: String
    )
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}