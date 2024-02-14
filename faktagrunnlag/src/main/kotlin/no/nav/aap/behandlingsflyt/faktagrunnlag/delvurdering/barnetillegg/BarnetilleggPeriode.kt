package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg

import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident

class BarnetilleggPeriode(
    val periode: Periode,
    val personIdenter: Set<Ident>
){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BarnetilleggPeriode

        if (periode != other.periode) return false
        if (!personIdenter.equals(other.personIdenter)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = periode.hashCode()
        result = 31 * result + personIdenter.hashCode()
        return result
    }
}