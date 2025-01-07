package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Faktagrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Repository

interface UnderveisRepository : Repository {
    fun hent(behandlingId: BehandlingId): UnderveisGrunnlag
    fun hentHvisEksisterer(behandlingId: BehandlingId): UnderveisGrunnlag?
    fun mapGrunnlag(row: Row): UnderveisGrunnlag
    fun mapPeriode(it: Row): Underveisperiode
    fun lagre(
        behandlingId: BehandlingId,
        underveisperioder: List<Underveisperiode>,
        input: Faktagrunnlag
    )

    fun lagreNyttGrunnlag(
        behandlingId: BehandlingId,
        underveisperioder: List<Underveisperiode>,
        input: Faktagrunnlag
    )

    fun deaktiverGrunnlag(behandlingId: BehandlingId)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}
