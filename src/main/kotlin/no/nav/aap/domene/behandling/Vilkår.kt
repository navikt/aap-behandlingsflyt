package no.nav.aap.domene.behandling

class Vilkår(
    val type: Vilkårstype
) {
    private val vilkårsperioder: MutableSet<Vilkårsperiode> = mutableSetOf()

    fun vilkårsperioder(): List<Vilkårsperiode> {
        return this.vilkårsperioder.toList()
    }

    fun leggTilVurdering(vilkårsperiode: Vilkårsperiode) {
        this.vilkårsperioder.add(vilkårsperiode)
        // TODO: Legg til overlappende constraint
    }

    override fun toString(): String {
        return "Vilkår(type=$type)"
    }
}
