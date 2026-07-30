package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDateTime

interface OvergangUføreRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): OvergangUføreGrunnlag?
    fun hentHistoriskeOvergangUføreVurderinger(sakId: SakId, behandlingId: BehandlingId): List<OvergangUføreVurdering>
    fun hentOvergangUføreVurderingPåTidspunkt(behandlingId: BehandlingId, tidspunkt: LocalDateTime): List<OvergangUføreVurdering>
    fun lagre(behandlingId: BehandlingId, overgangUføreVurderinger: List<OvergangUføreVurdering>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}