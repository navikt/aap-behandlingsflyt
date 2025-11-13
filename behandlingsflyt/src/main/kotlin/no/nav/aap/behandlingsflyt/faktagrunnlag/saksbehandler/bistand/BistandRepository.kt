package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface BistandRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): BistandGrunnlag?
    fun hentHistoriskeBistandsvurderinger(sakId: SakId, behandlingId: BehandlingId): List<Bistandsvurdering>
    fun lagre(behandlingId: BehandlingId, bistandsvurderinger: List<Bistandsvurdering>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}