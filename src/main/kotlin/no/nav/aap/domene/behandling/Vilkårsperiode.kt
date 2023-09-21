package no.nav.aap.domene.behandling

import no.nav.aap.domene.Periode

class Vilkårsperiode(
    val periode: Periode,
    val utfall: Utfall,
    val manuellVurdering: Boolean = false,
    val begrunnelse: String?,
    val avslagsårsak: Avslagsårsak? = null,
    internal val faktagrunnlag: Faktagrunnlag?,
    internal val beslutningstre: Beslutningstre?
) {
    constructor(
        periode: Periode,
        utfall: Utfall,
        manuellVurdering: Boolean,
        faktagrunnlag: Faktagrunnlag?,
        begrunnelse: String?,
        avslagsårsak: Avslagsårsak? = null,
    ) : this(periode, utfall, manuellVurdering, begrunnelse, avslagsårsak, faktagrunnlag, TomtBeslutningstre())

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
        if (beslutningstre != other.beslutningstre) return false

        return true
    }

    override fun hashCode(): Int {
        var result = periode.hashCode()
        result = 31 * result + utfall.hashCode()
        result = 31 * result + faktagrunnlag.hashCode()
        result = 31 * result + beslutningstre.hashCode()
        return result
    }

    fun erIkkeVurdert(): Boolean {
        return utfall !in setOf(Utfall.IKKE_OPPFYLT, Utfall.OPPFYLT)
    }
}
