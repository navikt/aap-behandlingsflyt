package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDateTime

interface ArbeidsevneRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): ArbeidsevneGrunnlag?
    fun hentArbeidsevneVurderingPåTidspunkt(behandlingId: BehandlingId, tidspunkt: LocalDateTime): List<ArbeidsevneVurdering>?
    fun lagre(behandlingId: BehandlingId, vurderinger: List<ArbeidsevneVurdering>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}