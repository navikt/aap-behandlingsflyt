package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.komponenter.type.Periode

/**
 * @param manuellVurdering Hvorvidt vilkåret ble utledet av systemet basert på fakta, eller om det ble manuelt satt av en saksbehandler.
 */
class Vilkårsperiode(
    val periode: Periode,
    val utfall: Utfall,
    val manuellVurdering: Boolean = false,
    val begrunnelse: String?,
    val innvilgelsesårsak: Innvilgelsesårsak? = null,
    val avslagsårsak: Avslagsårsak? = null,
    internal val faktagrunnlag: Faktagrunnlag? = null,
    val versjon: String = ApplikasjonsVersjon.versjon
) {

    internal constructor(periode: Periode, vilkårsvurdering: Vilkårsvurdering) : this(
        periode,
        vilkårsvurdering.utfall,
        vilkårsvurdering.manuellVurdering,
        vilkårsvurdering.begrunnelse,
        vilkårsvurdering.innvilgelsesårsak,
        vilkårsvurdering.avslagsårsak,
        vilkårsvurdering.faktagrunnlag,
        vilkårsvurdering.versjon
    )

    init {
        if (utfall == Utfall.IKKE_OPPFYLT && avslagsårsak == null) {
            throw IllegalStateException("Avslagsårsak må være satt ved IKKE_OPPFYLT som utfall")
        }
        if (avslagsårsak != null && innvilgelsesårsak != null) {
            throw IllegalStateException("Kan ikke sette både avslagsårsak ${avslagsårsak}og innvilgelsesårsak $innvilgelsesårsak.")
        }
    }

    fun erOppfylt(): Boolean {
        return utfall == Utfall.OPPFYLT
    }

    fun faktagrunnlag(): Faktagrunnlag? {
        return faktagrunnlag
    }

    override fun toString(): String {
        return "Vilkårsperiode(periode=$periode, utfall=$utfall, innvilgelsesårsak=$innvilgelsesårsak)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vilkårsperiode

        if (periode != other.periode) return false
        if (utfall != other.utfall) return false
        if (begrunnelse != other.begrunnelse) return false
        if (manuellVurdering != other.manuellVurdering) return false
        if (innvilgelsesårsak != other.innvilgelsesårsak) return false
        if (avslagsårsak != other.avslagsårsak) return false

        return true
    }

    override fun hashCode(): Int {
        var result = periode.hashCode()
        result = 31 * result + utfall.hashCode()
        result = 31 * result + begrunnelse.hashCode()
        result = 31 * result + manuellVurdering.hashCode()
        result = 31 * result + innvilgelsesårsak.hashCode()
        result = 31 * result + avslagsårsak.hashCode()
        return result
    }

    fun faktagrunnlagSomString(): String? {
        if (faktagrunnlag == null) {
            return null
        }

        return faktagrunnlag.hent()
    }
}