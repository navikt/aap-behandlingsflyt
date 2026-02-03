package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.Repository

interface RettighetstypeRepository : Repository {
    fun hent(behandlingId: BehandlingId): RettighetstypeGrunnlag
    fun hentHvisEksisterer(behandlingId: BehandlingId): RettighetstypeGrunnlag?
    fun lagre(
        behandlingId: BehandlingId,
        rettighetstypeMedKvoteVurderinger: Tidslinje<KvoteVurdering>,
        input: Faktagrunnlag
    )
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}