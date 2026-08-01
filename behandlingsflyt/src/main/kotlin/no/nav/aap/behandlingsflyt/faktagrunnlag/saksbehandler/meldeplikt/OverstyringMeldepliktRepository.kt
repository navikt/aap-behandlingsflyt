package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.meldeplikt.OverstyringMeldepliktGrunnlag
import no.nav.aap.meldeplikt.OverstyringMeldepliktVurdering

interface OverstyringMeldepliktRepository  : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): OverstyringMeldepliktGrunnlag?
    fun lagre(behandlingId: BehandlingId, vurdering: OverstyringMeldepliktVurdering)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    override fun slett(behandlingId: BehandlingId)
}