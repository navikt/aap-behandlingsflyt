package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Repository

interface ArbeidsopptrappingRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): ArbeidsopptrappingGrunnlag?
    fun hentPerioder(behandlingId: BehandlingId): List<Periode>
    fun lagre(behandlingId: BehandlingId, arbeidsopptrappingVurderinger: List<ArbeidsopptrappingVurdering>)
    override fun slett(behandlingId: BehandlingId)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}