package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.beregning.Prosent
import no.nav.aap.behandlingsflyt.flyt.vilkår.Utfall
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
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
        return vurderinger.isNotEmpty() && vurderinger.none { it.value == Utfall.IKKE_OPPFYLT }
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
        return "Vurdering(harRett=${harRett()}, gradering=${gradering?.prosent ?: Prosent(0)})"
    }

}
