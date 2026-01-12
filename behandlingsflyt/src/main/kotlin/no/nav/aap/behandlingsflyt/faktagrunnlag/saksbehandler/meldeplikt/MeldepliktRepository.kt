package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface MeldepliktRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktGrunnlag?
    fun hentAlleVurderinger(sakId: SakId, behandlingId: BehandlingId): Set<Fritaksvurdering>
    fun lagre(behandlingId: BehandlingId, vurderinger: List<Fritaksvurdering>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}