package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student

import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode

class StudentGrunnlag(
    val vurderinger: List<StudentVurdering>?,
    val oppgittStudent: OppgittStudent?
) {
    fun somTidslinje(rettighetsperiode: Periode): Tidslinje<StudentVurdering> {
        /* TODO: periodisering av studentvilkåret */
        val vurdering = vurderinger?.single()
        return if (vurdering == null) {
            Tidslinje.empty()
        } else {
            tidslinjeOf(rettighetsperiode to vurdering)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StudentGrunnlag

        return vurderinger == other.vurderinger
    }

    override fun hashCode(): Int {
        return vurderinger?.hashCode() ?: 0
    }
}

fun StudentGrunnlag?.vilkårIkkeOppfylt(): Boolean {
    return this?.vurderinger == null || !vurderinger.single().erOppfylt()
}

fun StudentGrunnlag?.søkerOppgirStudentstatus(): Boolean {
    return this?.oppgittStudent?.erStudent() == true
}