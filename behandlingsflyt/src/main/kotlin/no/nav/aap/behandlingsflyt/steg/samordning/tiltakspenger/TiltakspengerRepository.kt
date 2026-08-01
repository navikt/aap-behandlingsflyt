package no.nav.aap.behandlingsflyt.steg.samordning.tiltakspenger

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.steg.samordning.andrestatligeytelservurdering.gateway.TiltakspengerPeriode
import no.nav.aap.komponenter.repository.Repository

interface TiltakspengerRepository : Repository {
    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hent(behandlingId: BehandlingId) : List<TiltakspengerPeriode>
    fun lagre(behandlingId: BehandlingId, tiltakspengerPeriode: List<TiltakspengerPeriode>)
    fun slett(behandlingId: BehandlingId)
}