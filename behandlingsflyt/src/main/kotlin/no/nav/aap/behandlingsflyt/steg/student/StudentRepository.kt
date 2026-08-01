package no.nav.aap.behandlingsflyt.steg.student

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.student.OppgittStudent
import no.nav.aap.student.StudentGrunnlag
import no.nav.aap.student.StudentVurdering

interface StudentRepository : Repository {
    fun lagre(behandlingId: BehandlingId, oppgittStudent: OppgittStudent?)
    fun lagre(behandlingId: BehandlingId, vurderinger: Set<StudentVurdering>?)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    fun hentHvisEksisterer(behandlingId: BehandlingId): StudentGrunnlag?
    fun hent(behandlingId: BehandlingId): StudentGrunnlag
}