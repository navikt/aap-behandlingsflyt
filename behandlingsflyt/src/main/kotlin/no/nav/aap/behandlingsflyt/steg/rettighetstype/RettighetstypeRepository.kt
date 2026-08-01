package no.nav.aap.behandlingsflyt.steg.rettighetstype

import no.nav.aap.vilkårsresultat.RettighetsType
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.rettighetstype.RettighetstypeGrunnlag

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