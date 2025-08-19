package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface OverstyringMeldepliktRepository  : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): OverstyringMeldepliktGrunnlag?
    fun lagre(behandlingId: BehandlingId, vurdering: OverstyringMeldepliktVurdering)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    override fun slett(behandling: BehandlingId)
}