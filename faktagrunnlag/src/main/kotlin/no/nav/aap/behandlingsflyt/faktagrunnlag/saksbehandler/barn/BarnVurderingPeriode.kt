package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn

import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident

class BarnVurderingPeriode (
    val barn: Set<Ident>,
    val periode: Periode
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BarnVurderingPeriode

        if (barn != other.barn) return false
        if (periode != other.periode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = barn.hashCode()
        result = 31 * result + periode.hashCode()
        return result
    }
}