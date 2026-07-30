package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.komponenter.tidslinje.Tidslinje

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