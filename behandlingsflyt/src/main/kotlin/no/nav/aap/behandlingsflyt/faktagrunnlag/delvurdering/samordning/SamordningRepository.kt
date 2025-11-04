package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface SamordningRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningGrunnlag?
    fun lagre(behandlingId: BehandlingId, samordningPerioder: Set<SamordningPeriode>, input: Faktagrunnlag)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}