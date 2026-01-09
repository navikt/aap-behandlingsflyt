package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface ArbeidsevneRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): ArbeidsevneGrunnlag?
    fun lagre(behandlingId: BehandlingId, vurderinger: List<ArbeidsevneVurdering>)
    fun migrerArbeidsevne()
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}