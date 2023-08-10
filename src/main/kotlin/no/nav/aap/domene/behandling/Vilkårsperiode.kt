package no.nav.aap.domene.behandling

import no.nav.aap.domene.typer.Periode

class Vilkårsperiode(
    private val periode: Periode,
    private val utfall: Utfall,
    private val faktagrunnlag: Faktagrunnlag,
    private val besluttningstre: Beslutningstre
) {

    fun erOppfylt(): Boolean {
        return utfall == Utfall.OPPFYLT
    }

    override fun toString(): String {
        return "Vilkårsperiode(periode=$periode, utfall=$utfall)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vilkårsperiode

        if (periode != other.periode) return false
        if (utfall != other.utfall) return false
        if (faktagrunnlag != other.faktagrunnlag) return false
        if (besluttningstre != other.besluttningstre) return false

        return true
    }

    override fun hashCode(): Int {
        var result = periode.hashCode()
        result = 31 * result + utfall.hashCode()
        result = 31 * result + faktagrunnlag.hashCode()
        result = 31 * result + besluttningstre.hashCode()
        return result
    }


}
