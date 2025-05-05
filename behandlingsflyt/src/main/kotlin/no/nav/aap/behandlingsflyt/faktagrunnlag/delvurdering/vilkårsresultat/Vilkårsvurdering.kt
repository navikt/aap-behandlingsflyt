package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag

class Vilkårsvurdering(
    val utfall: Utfall,
    val manuellVurdering: Boolean,
    val begrunnelse: String?,
    val innvilgelsesårsak: Innvilgelsesårsak? = null,
    val avslagsårsak: Avslagsårsak? = null,
    internal val faktagrunnlag: Faktagrunnlag?,
    internal val versjon: String = ApplikasjonsVersjon.versjon
) {
    constructor(vilkårsperiode: Vilkårsperiode) : this(
        utfall = vilkårsperiode.utfall,
        manuellVurdering = vilkårsperiode.manuellVurdering,
        begrunnelse = vilkårsperiode.begrunnelse,
        innvilgelsesårsak = vilkårsperiode.innvilgelsesårsak,
        avslagsårsak = vilkårsperiode.avslagsårsak,
        faktagrunnlag = vilkårsperiode.faktagrunnlag,
        versjon = vilkårsperiode.versjon,
    )

    init {
        if (utfall == Utfall.IKKE_OPPFYLT && avslagsårsak == null) {
            throw IllegalStateException("Avslagsårsak må være satt ved IKKE_OPPFYLT som utfall")
        }
    }

    fun erOppfylt(): Boolean {
        return utfall == Utfall.OPPFYLT
    }

    fun erIkkeRelevant(): Boolean {
        return utfall == Utfall.IKKE_RELEVANT
    }

    fun erVurdert(): Boolean {
        return utfall != Utfall.IKKE_VURDERT
    }

    fun faktagrunnlag(): Faktagrunnlag? {
        return faktagrunnlag
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
            Utfall.OPPFYLT,
            Utfall.IKKE_RELEVANT
        )
    }

    fun faktagrunnlagSomString(): String? {
        if (faktagrunnlag == null) {
            return null
        }

        return faktagrunnlag.hent()
    }

    override fun toString(): String {
        return "Vilkårsvurdering(avslagsårsak=$avslagsårsak, utfall=$utfall, manuellVurdering=$manuellVurdering, begrunnelse=$begrunnelse, innvilgelsesårsak=$innvilgelsesårsak, faktagrunnlag=$faktagrunnlag, versjon='$versjon')"
    }
}