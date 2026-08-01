package no.nav.aap.student

import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.sykdom.Sykdomsvurdering

object StudentValidering {
    fun nårVurderingErKonsistentMedSykdom(
        studentTidslinje: Tidslinje<StudentVurdering>,
        sykdomstidslinje: Tidslinje<Sykdomsvurdering>
    ): Tidslinje<Boolean> {
        return Tidslinje.map2(studentTidslinje, sykdomstidslinje) { studentVurdering, sykdomsvurdering ->
            !(sykdomsvurdering?.potensieltOppfyltStudent() != true && studentVurdering?.erOppfylt() == true)
        }
    }
}