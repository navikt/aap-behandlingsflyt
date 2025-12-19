package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student

import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode

data class StudentGrunnlag(
    val vurderinger: Set<StudentVurdering>?,
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
}

fun StudentGrunnlag?.vilkårIkkeOppfylt(): Boolean {
    return this?.vurderinger == null || !vurderinger.single().erOppfylt()
}

fun StudentGrunnlag?.søkerOppgirStudentstatus(): Boolean {
    return this?.oppgittStudent?.erStudent() == true
}