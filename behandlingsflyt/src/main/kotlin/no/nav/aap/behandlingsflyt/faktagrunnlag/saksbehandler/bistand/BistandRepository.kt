package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import java.time.LocalDateTime
import no.nav.aap.bistand.BistandGrunnlag
import no.nav.aap.bistand.Bistandsvurdering

interface BistandRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): BistandGrunnlag?
    fun lagre(behandlingId: BehandlingId, bistandsvurderinger: List<Bistandsvurdering>)
    fun hentBistandsvurderingPåTidspunkt(behandlingId: BehandlingId, tidspunkt: LocalDateTime): List<Bistandsvurdering>?
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}