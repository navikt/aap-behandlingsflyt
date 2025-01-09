package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Faktagrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface UnderveisRepository : Repository {
    fun hent(behandlingId: BehandlingId): UnderveisGrunnlag
    fun hentHvisEksisterer(behandlingId: BehandlingId): UnderveisGrunnlag?
    fun lagre(
        behandlingId: BehandlingId,
        underveisperioder: List<Underveisperiode>,
        input: Faktagrunnlag
    )

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}
