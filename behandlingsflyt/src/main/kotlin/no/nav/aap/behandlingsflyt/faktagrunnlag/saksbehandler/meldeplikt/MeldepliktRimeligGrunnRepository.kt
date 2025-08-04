package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface MeldepliktRimeligGrunnRepository  : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktRimeligGrunnGrunnlag?
    fun hentAlleVurderinger(sakId: SakId, behandlingId: BehandlingId): Set<RimeligGrunnVurdering>
    fun lagre(behandlingId: BehandlingId, vurderinger: List<RimeligGrunnVurdering>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}