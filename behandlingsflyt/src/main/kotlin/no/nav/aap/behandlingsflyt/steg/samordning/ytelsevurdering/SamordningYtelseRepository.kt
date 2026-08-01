package no.nav.aap.behandlingsflyt.steg.samordning.ytelsevurdering

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.misc.SamordningYtelse
import no.nav.aap.misc.SamordningYtelseGrunnlag
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