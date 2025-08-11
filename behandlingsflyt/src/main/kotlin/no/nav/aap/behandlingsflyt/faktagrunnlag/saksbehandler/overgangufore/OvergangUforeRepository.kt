package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface OvergangUforeRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): OvergangUforeGrunnlag?
    fun hentHistoriskeOvergangUforeVurderinger(sakId: SakId, behandlingId: BehandlingId): List<OvergangUforeVurdering>
    fun lagre(behandlingId: BehandlingId, bistandsvurderinger: List<OvergangUforeVurdering>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}