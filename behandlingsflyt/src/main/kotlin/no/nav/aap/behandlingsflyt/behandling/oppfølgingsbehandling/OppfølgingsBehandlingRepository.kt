package no.nav.aap.behandlingsflyt.behandling.oppfølgingsbehandling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository


interface OppfølgingsBehandlingRepository : Repository {
    fun lagre(behandlingId: BehandlingId, grunnlag: OppfølgingsoppgaveGrunnlag)
    fun hent(behandlingId: BehandlingId): OppfølgingsoppgaveGrunnlag?
}