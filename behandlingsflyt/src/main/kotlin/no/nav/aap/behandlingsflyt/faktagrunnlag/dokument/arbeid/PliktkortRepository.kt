package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface PliktkortRepository : Repository{
    fun hent(behandlingId: BehandlingId): PliktkortGrunnlag
    fun hentHvisEksisterer(behandlingId: BehandlingId): PliktkortGrunnlag?
    fun lagre(behandlingId: BehandlingId, pliktkortene: Set<Pliktkort>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}