package no.nav.aap.behandlingsflyt.faktagrunnlag.register.tiltakspenger

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.repository.Repository

interface TiltakspengerRepository : Repository {
    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hent(behandlingId: BehandlingId) : List<TiltakspengerPeriode>
    fun lagre(behandlingId: BehandlingId, tiltakspengerPeriode: List<TiltakspengerPeriode>)
    fun slett(behandlingId: BehandlingId)
}