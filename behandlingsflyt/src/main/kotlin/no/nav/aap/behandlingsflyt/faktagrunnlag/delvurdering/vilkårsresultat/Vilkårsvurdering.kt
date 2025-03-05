package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag

class Vilkårsvurdering(vilkårsperiode: Vilkårsperiode) {
    val utfall: Utfall = vilkårsperiode.utfall
    val manuellVurdering: Boolean = vilkårsperiode.manuellVurdering
    val begrunnelse: String? = vilkårsperiode.begrunnelse
    val innvilgelsesårsak: Innvilgelsesårsak? = vilkårsperiode.innvilgelsesårsak
    val avslagsårsak: Avslagsårsak? = vilkårsperiode.avslagsårsak
    internal val faktagrunnlag: Faktagrunnlag? = vilkårsperiode.faktagrunnlag
    internal val versjon: String = vilkårsperiode.versjon

    init {
        if (utfall == Utfall.IKKE_OPPFYLT && avslagsårsak == null) {
            throw IllegalStateException("Avslagsårsak må være satt ved IKKE_OPPFYLT som utfall")
        }
    }

    fun erOppfylt(): Boolean {
        return utfall == Utfall.OPPFYLT
    }

    fun faktagrunnlag(): Faktagrunnlag? {
        return faktagrunnlag
    }

    override fun toString(): String {
        return "Vilkårsvurdering(utfall=$utfall)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vilkårsvurdering

        if (utfall != other.utfall) return false
        if (begrunnelse != other.begrunnelse) return false
        if (manuellVurdering != other.manuellVurdering) return false
        if (innvilgelsesårsak != other.innvilgelsesårsak) return false
        if (avslagsårsak != other.avslagsårsak) return false

        return true
    }

    override fun hashCode(): Int {
        var result = utfall.hashCode()
        result = 31 * result + begrunnelse.hashCode()
        result = 31 * result + manuellVurdering.hashCode()
        result = 31 * result + innvilgelsesårsak.hashCode()
        result = 31 * result + avslagsårsak.hashCode()
        return result
    }

    fun erIkkeVurdert(): Boolean {
        return utfall !in setOf(
            Utfall.IKKE_OPPFYLT,
            Utfall.OPPFYLT
        )
    }

    fun faktagrunnlagSomString(): String? {
        if (faktagrunnlag == null) {
            return null
        }

        return faktagrunnlag.hent()
    }
}