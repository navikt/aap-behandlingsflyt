package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryStudentRepository : StudentRepository {
    private val mutex = Any()
    private val grunnlag = HashMap<BehandlingId, StudentGrunnlag>()

    override fun lagre(
        behandlingId: BehandlingId,
        oppgittStudent: OppgittStudent?
    ) {
        synchronized(mutex) {
            grunnlag[behandlingId] = (grunnlag[behandlingId] ?: StudentGrunnlag(null, null))
                .copy(oppgittStudent = oppgittStudent)
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        vurderinger: Set<StudentVurdering>?
    ) {
        synchronized(mutex) {
            grunnlag[behandlingId] = (grunnlag[behandlingId] ?: StudentGrunnlag(null, null))
                .copy(vurderinger = vurderinger)
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        synchronized(mutex) {
            val fraGrunnlag = grunnlag[fraBehandling]
            if (fraGrunnlag != null) {
                grunnlag[tilBehandling] = fraGrunnlag
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): StudentGrunnlag? {
        return synchronized(mutex) {
            grunnlag[behandlingId]
        }
    }

    override fun hent(behandlingId: BehandlingId): StudentGrunnlag {
        return hentHvisEksisterer(behandlingId)!!
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(mutex) {
            grunnlag.remove(behandlingId)
        }
    }
}