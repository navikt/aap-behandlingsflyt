package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryStudentRepository : StudentRepository {
    override fun lagre(
        behandlingId: BehandlingId,
        oppgittStudent: OppgittStudent?
    ) {
        TODO("Not yet implemented")
    }

    override fun lagre(
        behandlingId: BehandlingId,
        vurderinger: Set<StudentVurdering>?
    ) {
        TODO("Not yet implemented")
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        TODO("Not yet implemented")
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): StudentGrunnlag? {
        TODO("Not yet implemented")
    }

    override fun hent(behandlingId: BehandlingId): StudentGrunnlag {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
        TODO("Not yet implemented")
    }
}