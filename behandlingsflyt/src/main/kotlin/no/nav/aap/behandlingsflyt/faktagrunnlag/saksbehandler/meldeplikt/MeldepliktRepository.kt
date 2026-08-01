package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDateTime
import no.nav.aap.meldeplikt.Fritaksvurdering
import no.nav.aap.meldeplikt.MeldepliktGrunnlag

interface MeldepliktRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktGrunnlag?
    fun lagre(behandlingId: BehandlingId, vurderinger: List<Fritaksvurdering>)
    fun hentFritaksvurderingPåTidspunkt(behandlingId: BehandlingId, tidspunkt: LocalDateTime): List<Fritaksvurdering>?
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}