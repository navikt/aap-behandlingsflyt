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
     *
     * @return En tidslinje med [RettighetsType].
     */
    fun rettighetstypeTidslinje(): Tidslinje<RettighetsType> {
        return listOf(
            Vilkårtype.SYKEPENGEERSTATNING,
            Vilkårtype.SYKDOMSVILKÅRET,
            Vilkårtype.BISTANDSVILKÅRET
        )
            .mapNotNull { v -> this.optionalVilkår(v) }
            .map { vilkår ->
                Tidslinje(vilkår.vilkårsperioder().filter { it.utfall == Utfall.OPPFYLT }
                    .map { Segment(it.periode, Pair(vilkår, it)) })
            }
            .map { it.filter { it.verdi.second.erOppfylt() } }
            .fold(Tidslinje.empty<Set<Pair<Vilkårtype, Innvilgelsesårsak?>>>()) { acc, curr ->
                acc.kombiner(curr, JoinStyle.OUTER_JOIN { periode, venstre, høyre ->
                    if (høyre == null && venstre == null) {
                        null
                    } else if (venstre != null && høyre == null) {
                        Segment(periode, venstre.verdi)
                    } else if (venstre == null && høyre != null) {
                        Segment(periode, setOf(Pair(høyre.verdi.first.type, høyre.verdi.second.innvilgelsesårsak)))
                    } else {
                        // Rart kompilatoren ikke skjønte dette...
                        requireNotNull(høyre)
                        requireNotNull(venstre)
                        Segment(
                            periode,
                            venstre.verdi + Pair(høyre.verdi.first.type, høyre.verdi.second.innvilgelsesårsak)
                        )
                    }
                })
            }
            .filter { it.verdi.any { it.first == Vilkårtype.BISTANDSVILKÅRET } }
            .mapValue { prioriterVilkår(it) }
            .komprimer()
    }

    /**
     * Prioriterer oppfylte vilkår for å utlede [RettighetsType].
     *
     * Nåværende implementasjon prioriterer spesielle innvilgelsesårsaker på bistandvilkåret. Om ingen
     * spesielle innvilgelsesårsaker er funnet, så sjekkes sykepengervilkåret. Om det er oppfylt, så
     * returneres [RettighetsType.SYKEPENGEERSTATNING].
     *
     * Default-verdien er [RettighetsType.BISTANDSBEHOV] (normal § 11-6).
     */
    private fun prioriterVilkår(vilkårPar: Set<Pair<Vilkårtype, Innvilgelsesårsak?>>): RettighetsType {
        val bistandVilkåret =
            requireNotNull(vilkårPar.firstOrNull { it.first == Vilkårtype.BISTANDSVILKÅRET })
            { "Bistandsvilkåret må være oppfylt for å regne ut rettighetstype." }

        if (bistandVilkåret.second != null) {
            val innvilgelsesårsak = requireNotNull(bistandVilkåret.second)
            when (innvilgelsesårsak) {
                Innvilgelsesårsak.STUDENT -> return RettighetsType.STUDENT
                Innvilgelsesårsak.ARBEIDSSØKER -> return RettighetsType.ARBEIDSSØKER
                Innvilgelsesårsak.VURDERES_FOR_UFØRETRYGD -> return RettighetsType.VURDERES_FOR_UFØRETRYGD
                Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG -> {}
            }
        }

        val sykepengervilkåret = vilkårPar.find { it.first == Vilkårtype.SYKEPENGEERSTATNING }

        if (sykepengervilkåret != null) {
            return RettighetsType.SYKEPENGEERSTATNING
        }

        return RettighetsType.BISTANDSBEHOV
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

