package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface SamordningVurderingRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningVurderingGrunnlag?
    fun hentHistoriskeVurderinger(
        sakId: SakId,
        behandlingId: BehandlingId,
        ekskluderteBehandlingIdListe: List<BehandlingId>
    ): List<SamordningVurderingGrunnlag>

    fun lagreVurderinger(
        behandlingId: BehandlingId,
        samordningVurderinger: SamordningVurderingGrunnlag
    )

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}

interface SamordningYtelseRepository : Repository {
    /**
     * Henter nyeste grunnlag fra register (den unike med aktiv = true).
     */
    fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningYtelseGrunnlag?

    /**
     * Henter eldste grunnlag på gjeldende behandling.
     */
    fun hentEldsteGrunnlag(behandlingId: BehandlingId): SamordningYtelseGrunnlag?
    fun lagre(behandlingId: BehandlingId, samordningYtelser: List<SamordningYtelse>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}