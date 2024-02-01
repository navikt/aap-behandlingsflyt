package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

import no.nav.aap.verdityper.Periode

class Yrkesskade(val ref: String, val periode: Periode){
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Yrkesskade

        if (ref != other.ref) return false
        if (periode != other.periode) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ref.hashCode()
        result = 31 * result + periode.hashCode()
        return result
    }
}
