package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.Repository

interface RettighetstypeRepository : Repository {
    fun hent(behandlingId: BehandlingId): RettighetstypeGrunnlag
    fun hentHvisEksisterer(behandlingId: BehandlingId): RettighetstypeGrunnlag?
    fun lagre(
        behandlingId: BehandlingId,
        rettighetstypeTidslinje: Tidslinje<RettighetsType>,
        faktagrunnlag: RettighetstypeFaktagrunnlag,
        versjon: String
    )
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}