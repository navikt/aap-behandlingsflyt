package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface Effektuer11_7Repository: Repository {
    fun lagreVurdering(behandlingId: BehandlingId, vurdering: Effektuer11_7Vurdering)
    fun lagreVarsel(behandlingId: BehandlingId, varsel: Effektuer11_7Forhåndsvarsel, underveisGrunnlag: UnderveisGrunnlag)
    fun hentHvisEksisterer(behandlingId: BehandlingId): Effektuer11_7Grunnlag?

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}