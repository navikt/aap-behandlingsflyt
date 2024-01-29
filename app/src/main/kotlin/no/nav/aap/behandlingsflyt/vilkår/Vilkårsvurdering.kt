package no.nav.aap.behandlingsflyt.vilkår

class Vilkårsvurdering(vilkårsperiode: no.nav.aap.behandlingsflyt.vilkår.Vilkårsperiode) {
    val utfall: no.nav.aap.behandlingsflyt.vilkår.Utfall
    val manuellVurdering: Boolean
    val begrunnelse: String?
    val innvilgelsesårsak: no.nav.aap.behandlingsflyt.vilkår.Innvilgelsesårsak?
    val avslagsårsak: no.nav.aap.behandlingsflyt.vilkår.Avslagsårsak?
    internal val faktagrunnlag: no.nav.aap.behandlingsflyt.vilkår.Faktagrunnlag?
    internal val versjon: String

    init {
        utfall = vilkårsperiode.utfall
        manuellVurdering = vilkårsperiode.manuellVurdering
        begrunnelse = vilkårsperiode.begrunnelse
        innvilgelsesårsak = vilkårsperiode.innvilgelsesårsak
        avslagsårsak = vilkårsperiode.avslagsårsak
        faktagrunnlag = vilkårsperiode.faktagrunnlag
        versjon = vilkårsperiode.versjon

        if (utfall == no.nav.aap.behandlingsflyt.vilkår.Utfall.IKKE_OPPFYLT && avslagsårsak == null) {
            throw IllegalStateException("Avslagsårsak må være satt ved IKKE_OPPFYLT som utfall")
        }
    }

    fun erOppfylt(): Boolean {
        return utfall == no.nav.aap.behandlingsflyt.vilkår.Utfall.OPPFYLT
    }

    fun faktagrunnlag(): no.nav.aap.behandlingsflyt.vilkår.Faktagrunnlag? {
        return faktagrunnlag
    }

    override fun toString(): String {
        return "Vilkårsvurdering(utfall=$utfall)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as no.nav.aap.behandlingsflyt.vilkår.Vilkårsperiode

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
            no.nav.aap.behandlingsflyt.vilkår.Utfall.IKKE_OPPFYLT,
            no.nav.aap.behandlingsflyt.vilkår.Utfall.OPPFYLT
        )
    }

    fun faktagrunnlagSomString(): String? {
        if (faktagrunnlag == null) {
            return null
        }

        return faktagrunnlag.hent()
    }
}