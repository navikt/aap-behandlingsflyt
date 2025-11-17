package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student

import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode

class StudentGrunnlag(
    val studentvurdering: StudentVurdering?,
    val oppgittStudent: OppgittStudent?
) {
    fun somTidslinje(rettighetsperiode: Periode): Tidslinje<StudentVurdering> {
        /* TODO: periodisering av studentvilkåret */
        return if (studentvurdering == null) {
            Tidslinje.empty()
        } else {
            tidslinjeOf(rettighetsperiode to studentvurdering)
        }
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

fun StudentGrunnlag?.søkerOppgirStudentstatus(): Boolean {
    return this?.oppgittStudent?.erStudent() == true
}