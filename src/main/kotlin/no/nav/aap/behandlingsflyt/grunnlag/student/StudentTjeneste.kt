package no.nav.aap.behandlingsflyt.grunnlag.student

import java.util.concurrent.atomic.AtomicLong
import no.nav.aap.behandlingsflyt.avklaringsbehov.student.StudentVurdering
import no.nav.aap.behandlingsflyt.domene.behandling.Behandling

object StudentTjeneste {

    private var grunnlagene = HashMap<Long, StudentGrunnlag>()

    private val key = AtomicLong()
    private val LOCK = Object()

    fun lagre(behandlingId: Long, studentvurdering: StudentVurdering?) {
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

    fun kopier(fraBehandling: Behandling, tilBehandling: Behandling) {
        synchronized(LOCK) {
            grunnlagene[fraBehandling.id]?.let { eksisterendeGrunnlag ->
                grunnlagene[tilBehandling.id] = eksisterendeGrunnlag
            }
        }
    }

    fun hentHvisEksisterer(behandlingId: Long): StudentGrunnlag? {
        synchronized(LOCK) {
            return grunnlagene[behandlingId]
        }
    }

    fun hent(behandlingId: Long): StudentGrunnlag {
        synchronized(LOCK) {
            return grunnlagene.getValue(behandlingId)
        }
    }
}
