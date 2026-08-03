package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDateTime

interface MeldepliktRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktGrunnlag?
    fun lagre(behandlingId: BehandlingId, vurderinger: List<Fritaksvurdering>)
    fun hentFritaksvurderingPåTidspunkt(behandlingId: BehandlingId, tidspunkt: LocalDateTime): List<Fritaksvurdering>?
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}