package no.nav.aap.behandlingsflyt.steg.samordning

import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.samordning.SamordningGrunnlag
import no.nav.aap.samordning.SamordningPeriode

interface SamordningRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningGrunnlag?
    fun lagre(behandlingId: BehandlingId, samordningPerioder: Set<SamordningPeriode>, input: Faktagrunnlag)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}
