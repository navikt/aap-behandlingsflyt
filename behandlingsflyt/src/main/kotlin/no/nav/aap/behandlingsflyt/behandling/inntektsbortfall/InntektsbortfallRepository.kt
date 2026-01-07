package no.nav.aap.behandlingsflyt.behandling.inntektsbortfall

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.InntektsbortfallVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface InntektsbortfallRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vurdering: InntektsbortfallVurdering)
    fun deaktiverGjeldendeVurdering(behandlingId: BehandlingId)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hentHvisEksisterer(behandlingId: BehandlingId): InntektsbortfallVurdering?
}