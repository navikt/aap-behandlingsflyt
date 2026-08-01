package no.nav.aap.behandlingsflyt.steg.overgangufore

import java.time.LocalDateTime
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.overganguføre.OvergangUføreGrunnlag
import no.nav.aap.overganguføre.OvergangUføreVurdering

interface OvergangUføreRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): OvergangUføreGrunnlag?
    fun hentHistoriskeOvergangUføreVurderinger(sakId: SakId, behandlingId: BehandlingId): List<OvergangUføreVurdering>
    fun hentOvergangUføreVurderingPåTidspunkt(behandlingId: BehandlingId, tidspunkt: LocalDateTime): List<OvergangUføreVurdering>
    fun lagre(behandlingId: BehandlingId, overgangUføreVurderinger: List<OvergangUføreVurdering>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}