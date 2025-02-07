package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface StudentRepository : Repository {
    fun lagre(behandlingId: BehandlingId, oppgittStudent: OppgittStudent?)
    fun lagre(behandlingId: BehandlingId, studentvurdering: StudentVurdering?)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hentHvisEksisterer(behandlingId: BehandlingId): StudentGrunnlag?
    fun hent(behandlingId: BehandlingId): StudentGrunnlag
}