package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface SamordningYtelseRepository : Repository {
    /**
     * Henter nyeste grunnlag fra register (den unike med aktiv = true).
     */
    fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningYtelseGrunnlag?

    /**
     * Henter eldste grunnlag på gjeldende behandling.
     */
    fun hentEldsteGrunnlag(behandlingId: BehandlingId): SamordningYtelseGrunnlag?
    fun lagre(behandlingId: BehandlingId, samordningYtelser: Set<SamordningYtelse>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}