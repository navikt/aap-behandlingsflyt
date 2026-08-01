package no.nav.aap.behandlingsflyt.steg.overgangarbeid

import java.time.LocalDateTime
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.overgangarbeid.OvergangArbeidGrunnlag
import no.nav.aap.overgangarbeid.OvergangArbeidVurdering

interface OvergangArbeidRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): OvergangArbeidGrunnlag?
    fun hentOvergangArbeidVurderingPåTidspunkt(behandlingId: BehandlingId, tidspunkt: LocalDateTime): List<OvergangArbeidVurdering>?
    fun lagre(behandlingId: BehandlingId, overgangArbeidVurderinger: List<OvergangArbeidVurdering>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}