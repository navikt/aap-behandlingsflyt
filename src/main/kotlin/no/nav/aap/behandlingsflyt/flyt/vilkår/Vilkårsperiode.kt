package no.nav.aap.behandlingsflyt.flyt.vilkår

import no.nav.aap.behandlingsflyt.Periode

class Vilkårsperiode(
    val periode: Periode,
    val utfall: Utfall,
    val manuellVurdering: Boolean = false,
    val begrunnelse: String?,
    val innvilgelsesårsak: Innvilgelsesårsak? = null,
    val avslagsårsak: Avslagsårsak? = null,
    internal val faktagrunnlag: Faktagrunnlag?,
    internal val versjon: String
) {
    constructor(
        periode: Periode,
        utfall: Utfall,
        manuellVurdering: Boolean,
        faktagrunnlag: Faktagrunnlag?,
        begrunnelse: String?,
        avslagsårsak: Avslagsårsak? = null,
        innvilgelsesårsak: Innvilgelsesårsak? = null,
    ) : this(
        periode,
        utfall,
        manuellVurdering,
        begrunnelse,
        innvilgelsesårsak,
        avslagsårsak,
        faktagrunnlag,
        ApplikasjonsVersjon.versjon
    )

    init {
        if (utfall == Utfall.IKKE_OPPFYLT && avslagsårsak == null) {
            throw IllegalStateException("Avslagsårsak må være satt ved IKKE_OPPFYLT som utfall")
        }
    }

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

        return true
    }

    override fun hashCode(): Int {
        var result = periode.hashCode()
        result = 31 * result + utfall.hashCode()
        result = 31 * result + faktagrunnlag.hashCode()
        return result
    }

    fun erIkkeVurdert(): Boolean {
        return utfall !in setOf(Utfall.IKKE_OPPFYLT, Utfall.OPPFYLT)
    }
}
