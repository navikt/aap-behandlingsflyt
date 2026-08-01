package no.nav.aap.behandlingsflyt.steg.inntektsbortfall

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.beregning.InntektsbortfallVurdering
import no.nav.aap.lookup.repository.Repository

interface InntektsbortfallRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vurdering: InntektsbortfallVurdering)
    fun deaktiverGjeldendeVurdering(behandlingId: BehandlingId)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hentHvisEksisterer(behandlingId: BehandlingId): InntektsbortfallVurdering?
}