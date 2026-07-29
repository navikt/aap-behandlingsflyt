package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDateTime

interface OvergangArbeidRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): OvergangArbeidGrunnlag?
    fun hentOvergangArbeidVurderingPåTidspunkt(behandlingId: BehandlingId, tidspunkt: LocalDateTime): List<OvergangArbeidVurdering>?
    fun lagre(behandlingId: BehandlingId, overgangArbeidVurderinger: List<OvergangArbeidVurdering>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}