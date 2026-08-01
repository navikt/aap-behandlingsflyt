package no.nav.aap.behandlingsflyt.steg.arbeidsopptrapping

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDateTime
import no.nav.aap.arbeidsopptrapping.ArbeidsopptrappingGrunnlag
import no.nav.aap.arbeidsopptrapping.ArbeidsopptrappingVurdering

interface ArbeidsopptrappingRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): ArbeidsopptrappingGrunnlag?
    fun lagre(behandlingId: BehandlingId, arbeidsopptrappingVurderinger: List<ArbeidsopptrappingVurdering>)
    fun hentArbeidsopptrappingVurderingPåTidspunkt(behandlingId: BehandlingId, tidspunkt: LocalDateTime): List<ArbeidsopptrappingVurdering>?
    override fun slett(behandlingId: BehandlingId)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}