package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.underveis.UnderveisAvslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårtype
import no.nav.aap.verdityper.Prosent
import java.util.*

class Vurdering(
    private val vurderinger: EnumMap<Vilkårtype, Utfall> = EnumMap(Vilkårtype::class.java),
    private val gradering: Gradering? = null,
    private val grenseverdi: Prosent? = null
) {

    fun leggTilVurdering(vilkårtype: Vilkårtype, utfall: Utfall): Vurdering {
        val kopi = EnumMap(vurderinger)
        kopi[vilkårtype] = utfall
        return Vurdering(
            vurderinger = kopi,
            gradering = gradering,
            grenseverdi = grenseverdi
        )
    }

    fun leggTilGradering(gradering: Gradering): Vurdering {
        return Vurdering(
            vurderinger = this.vurderinger,
            gradering = gradering,
            grenseverdi = this.grenseverdi
        )
    }

    fun leggTilGrenseverdi(grenseverdi: Prosent): Vurdering {
        return Vurdering(
            vurderinger = this.vurderinger,
            gradering = this.gradering,
            grenseverdi = grenseverdi
        )
    }

    fun vurderinger(): Map<Vilkårtype, Utfall> {
        return vurderinger.toMap()
    }

    fun harRett(): Boolean {
        return ingenVilkårErAvslått() && arbeiderMindreEnnGrenseverdi()
    }

    private fun ingenVilkårErAvslått(): Boolean {
        return vurderinger.isNotEmpty() && vurderinger.none { it.value == Utfall.IKKE_OPPFYLT }
    }

    private fun arbeiderMindreEnnGrenseverdi(): Boolean {
        return gradering == null || grenseverdi() >= gradering.andelArbeid
    }

    fun grenseverdi(): Prosent {
        return requireNotNull(grenseverdi)
    }

    fun gradering(): Gradering? {
        return gradering
    }

    fun utfall(): Utfall {
        return if (harRett()) {
            Utfall.OPPFYLT
        } else {
            Utfall.IKKE_OPPFYLT
        }
    }

    fun avslagsårsak(): UnderveisAvslagsårsak? {
        if (harRett()) {
            return null
        }

        if (!ingenVilkårErAvslått()) {
            return UnderveisAvslagsårsak.IKKE_GRUNNLEGGENDE_RETT
        } else if (!arbeiderMindreEnnGrenseverdi()) {
            return UnderveisAvslagsårsak.ARBEIDER_MER_ENN_GRENSEVERDI
        }
        throw IllegalStateException("Ukjent avslagsårsak")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vurdering

        if (vurderinger != other.vurderinger) return false
        if (gradering != other.gradering) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vurderinger.hashCode()
        result = 31 * result + (gradering?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Vurdering(harRett=${harRett()}, gradering=${gradering?.gradering ?: Prosent(0)})"
    }

}
