package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje

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

    fun optionalVilkår(vilkårtype: Vilkårtype): Vilkår? {
        return vilkår.firstOrNull { it.type == vilkårtype }
    }

    fun alle(): List<Vilkår> {
        return vilkår.toList()
    }

    /**
     * Går gjennom oppfylte vilkår over alle perioder, og utleder hjemmel.
     */
    fun rettighetstypeTidslinje(): Tidslinje<String> {
        var other = Tidslinje.empty<Set<Pair<Vilkårtype, Innvilgelsesårsak?>>>()
        this.alle().filter {
            it.type in setOf(
                Vilkårtype.BISTANDSVILKÅRET,
                Vilkårtype.SYKDOMSVILKÅRET,
                Vilkårtype.SYKEPENGEERSTATNING
            )
        }
            .map { vilkår -> Tidslinje(vilkår.vilkårsperioder().map { Segment(it.periode, Pair(vilkår, it)) }) }
            .forEach {
                other = other.kombiner(it, JoinStyle.RIGHT_JOIN { periode, venstre, høyre ->
                    if (høyre.verdi.second.utfall != Utfall.OPPFYLT) {
                        null
                    } else {
                        val element = Pair(høyre.verdi.first.type, høyre.verdi.second.innvilgelsesårsak)
                        if (venstre == null) {
                            Segment(periode, setOf(element))
                        } else {
                            Segment(
                                periode,
                                venstre.verdi + element
                            )
                        }
                    }
                })
            }


        return other.mapValue {
            it.prioriterVilkår()
        }
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

fun Set<Pair<Vilkårtype, Innvilgelsesårsak?>>.prioriterVilkår(): String {
    val spesielle = this.filter { it.second != null }

    if (spesielle.isNotEmpty()) {
        return spesielle.first().second!!.kode
    }

    return this.first { it.second == null }.first.kode
}