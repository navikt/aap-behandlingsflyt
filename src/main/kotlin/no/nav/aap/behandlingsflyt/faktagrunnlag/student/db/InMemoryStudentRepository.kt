package no.nav.aap.behandlingsflyt.faktagrunnlag.student.db

import no.nav.aap.behandlingsflyt.avklaringsbehov.student.StudentVurdering
import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentRepository
import java.util.concurrent.atomic.AtomicLong

object InMemoryStudentRepository : StudentRepository {

    private var grunnlagene = HashMap<Long, StudentGrunnlag>()

    private val key = AtomicLong()
    private val LOCK = Object()

    override fun lagre(behandlingId: Long, studentvurdering: StudentVurdering?) {
        synchronized(LOCK) {
            grunnlagene.put(
                behandlingId,
                StudentGrunnlag(
                    behandlingId = behandlingId,
                    studentvurdering = studentvurdering,
                    id = key.addAndGet(1)
                )
            )
        }
    }

    override fun kopier(fraBehandling: Behandling, tilBehandling: Behandling) {
        synchronized(LOCK) {
            grunnlagene[fraBehandling.id]?.let { eksisterendeGrunnlag ->
                grunnlagene[tilBehandling.id] = eksisterendeGrunnlag
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: Long): StudentGrunnlag? {
        synchronized(LOCK) {
            return grunnlagene[behandlingId]
        }
    }

    override fun hent(behandlingId: Long): StudentGrunnlag {
        synchronized(LOCK) {
            return grunnlagene.getValue(behandlingId)
        }
    }
}
