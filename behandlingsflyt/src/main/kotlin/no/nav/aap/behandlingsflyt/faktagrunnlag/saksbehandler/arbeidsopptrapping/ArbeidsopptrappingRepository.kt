package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface ArbeidsopptrappingRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): ArbeidsopptrappingGrunnlag?
    fun hentAlleVurderinger(sakId: SakId, behandlingId: BehandlingId): Set<ArbeidsopptrappingVurdering>
    fun lagre(behandlingId: BehandlingId, arbeidsopptrappingVurderinger: List<ArbeidsopptrappingVurdering>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}