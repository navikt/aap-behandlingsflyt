package no.nav.aap.behandlingsflyt.steg.krav

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.krav.KravGrunnlag
import no.nav.aap.krav.KravVurdering
import no.nav.aap.lookup.repository.Repository

interface KravRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vurderinger: Set<KravVurdering>)
    fun hent(behandlingId: BehandlingId): KravGrunnlag
    fun hentHvisEksisterer(behandlingId: BehandlingId): KravGrunnlag?
    override fun slett(behandlingId: BehandlingId)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}