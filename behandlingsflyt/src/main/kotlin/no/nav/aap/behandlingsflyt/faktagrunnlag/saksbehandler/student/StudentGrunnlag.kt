package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student

class StudentGrunnlag(
    val id: Long? = null,
    val studentvurdering: StudentVurdering?,
    val oppgittStudent: OppgittStudent?
) {
    fun erKonsistent(): Boolean {
        if (oppgittStudent == null || !oppgittStudent.erStudent()) {
            return true
        }
        return studentvurdering != null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StudentGrunnlag

        return studentvurdering == other.studentvurdering
    }

    override fun hashCode(): Int {
        return studentvurdering?.hashCode() ?: 0
    }
}

fun StudentGrunnlag?.vilkårIkkeOppfylt(): Boolean {
    return this?.studentvurdering == null || !studentvurdering.erOppfylt()
}
