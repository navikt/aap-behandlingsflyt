package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface ManuellInntektGrunnlagRepository : Repository {
    fun lagre(behandlingId: BehandlingId, manuellVurdering: ManuellInntektVurdering)
    fun lagre(behandlingId: BehandlingId, manuellVurderinger: Set<ManuellInntektVurdering>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hentHvisEksisterer(behandlingId: BehandlingId): ManuellInntektGrunnlag?
    fun hentHistoriskeVurderinger(sakId: SakId, behandlingId: BehandlingId): List<ManuellInntektVurdering>
}