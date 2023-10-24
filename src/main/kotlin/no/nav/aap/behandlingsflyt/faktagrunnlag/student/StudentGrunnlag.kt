package no.nav.aap.behandlingsflyt.faktagrunnlag.student

import no.nav.aap.behandlingsflyt.avklaringsbehov.student.StudentVurdering

class StudentGrunnlag(
        val id: Long,
        val behandlingId: Long,
        val studentvurdering: StudentVurdering?,
) {
    fun erKonsistent(): Boolean {
        // TODO: Kommenter inn når lagring er på plass
//        if (oppgittStudent == null) {
//            return true
//        }
        return studentvurdering != null
    }
}
