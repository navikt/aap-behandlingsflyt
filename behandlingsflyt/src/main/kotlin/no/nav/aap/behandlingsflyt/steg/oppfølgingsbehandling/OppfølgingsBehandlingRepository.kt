package no.nav.aap.behandlingsflyt.steg.oppfølgingsbehandling

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface OppfølgingsBehandlingRepository : Repository {
    fun lagre(behandlingId: BehandlingId, grunnlag: OppfølgingsoppgaveGrunnlag)
    fun hent(behandlingId: BehandlingId): OppfølgingsoppgaveGrunnlag?
}