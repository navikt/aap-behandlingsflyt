package no.nav.aap.behandlingsflyt.flyt.vilkår

class Vilkårsresultat(
    internal var id: Long? = null,
    vilkår: List<Vilkår> = emptyList()
) {
    private val vilkår: MutableList<Vilkår> = vilkår.toMutableList()

    fun leggTilHvisIkkeEksisterer(vilkårtype: Vilkårtype): Vilkår {
        if (vilkår.none { it.type == vilkårtype }) {
            this.vilkår.add(Vilkår(type = vilkårtype))
        }
        return finnVilkår(vilkårtype)
    }

    fun finnVilkår(vilkårtype: Vilkårtype): Vilkår {
        return vilkår.first { it.type == vilkårtype }
    }

    fun alle(): List<Vilkår> {
        return vilkår.toList()
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
}
