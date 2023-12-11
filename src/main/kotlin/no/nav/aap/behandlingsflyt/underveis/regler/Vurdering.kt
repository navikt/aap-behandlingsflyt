package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.beregning.Prosent
import no.nav.aap.behandlingsflyt.flyt.vilkår.Utfall
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype

class Vurdering {
    private val vurderinger: HashMap<Vilkårtype, Utfall> = HashMap()
    private var gradering: Gradering? = null

    fun leggTilVurdering(vilkårtype: Vilkårtype, utfall: Utfall) {
        vurderinger[vilkårtype] = utfall
    }

    fun leggTilGradering(gradering: Gradering) {
        this.gradering = gradering
    }

    fun vurderinger(): Map<Vilkårtype, Utfall> {
        return vurderinger
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
