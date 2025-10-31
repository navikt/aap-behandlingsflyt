package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.vurderRettighetsType
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty

class Vilkårsresultat(
    internal var id: Long? = null,
    vilkår: List<Vilkår> = emptyList()
) {
    init {
        require(vilkår.distinctBy { it.type }.size == vilkår.size)
    }

    private val vilkår: MutableList<Vilkår> = vilkår.toMutableList()

    fun leggTilHvisIkkeEksisterer(vilkårtype: Vilkårtype): Vilkår {
        if (vilkår.none { it.type == vilkårtype }) {
            this.vilkår.add(Vilkår(type = vilkårtype))
        }
        return finnVilkår(vilkårtype)
    }

    fun finnVilkår(vilkårtype: Vilkårtype): Vilkår {
        return requireNotNull(optionalVilkår(vilkårtype)) {
            "Finner ikke $vilkårtype blant ${vilkår.joinToString { it.type.name }} (Vilkårsresultat.id=${id})."
        }
    }

    fun optionalVilkår(vilkårtype: Vilkårtype): Vilkår? {
        return vilkår.firstOrNull { it.type == vilkårtype }
    }

    fun alle(): List<Vilkår> {
        return vilkår.toList()
    }

    fun rettighetstypeTidslinje(): Tidslinje<RettighetsType> {
        return vurderRettighetsType(this)
    }

    fun tidslinjeFor(vilkårstype: Vilkårtype): Tidslinje<Vilkårsvurdering> {
        return optionalVilkår(vilkårstype)?.tidslinje().orEmpty()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vilkårsresultat

        return vilkår == other.vilkår
    }

    override fun hashCode(): Int {
        return vilkår.hashCode()
    }

    override fun toString(): String {
        return "Vilkårsresultat(id=$id, vilkår=$vilkår)"
    }
}