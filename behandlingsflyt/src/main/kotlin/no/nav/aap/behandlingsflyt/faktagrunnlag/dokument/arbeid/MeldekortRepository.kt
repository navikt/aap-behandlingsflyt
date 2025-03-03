package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface MeldekortRepository : Repository{
    fun hent(behandlingId: BehandlingId): MeldekortGrunnlag
    fun hentHvisEksisterer(behandlingId: BehandlingId): MeldekortGrunnlag?
    fun lagre(behandlingId: BehandlingId, meldekortene: Set<Meldekort>)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}