package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface EtableringEgenVirksomhetRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): EtableringEgenVirksomhetGrunnlag?
    fun lagre(behandlingId: BehandlingId, etableringEgenvirksomhetVurderinger: List<EtableringEgenVirksomhetVurdering>)
    override fun slett(behandlingId: BehandlingId)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}