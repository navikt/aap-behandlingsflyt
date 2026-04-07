package no.nav.aap.behandlingsflyt.faktagrunnlag.register.dagpenger

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.repository.Repository

interface DagpengerRepository : Repository {
    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hent(behandlingId: BehandlingId) : List<DagpengerPeriode>
    fun lagre(behandlingId: BehandlingId, dagpengerPeriode: List<DagpengerPeriode>)
    fun slett(behandlingId: BehandlingId)
}