package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
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

    /**
     * Går gjennom oppfylte vilkår over alle perioder, og utleder hjemmel.
     *
     * @return En tidslinje med [RettighetsType].
     */
    fun rettighetstypeTidslinje(): Tidslinje<RettighetsType> {
        require(vilkår.any { it.type == Vilkårtype.ALDERSVILKÅRET })
        require(vilkår.any { it.type == Vilkårtype.BISTANDSVILKÅRET })
        require(vilkår.any { it.type == Vilkårtype.MEDLEMSKAP })
        require(vilkår.any { it.type == Vilkårtype.LOVVALG })
        require(vilkår.any { it.type == Vilkårtype.SYKDOMSVILKÅRET })
        require(vilkår.distinctBy { it.type }.size == vilkår.size)

        return vilkår
            .map { vilkår -> vilkår.tidslinje().mapValue { vurdering -> vilkår to vurdering } }
            .outerJoin()
            .filter { vilkåreneSegment ->
                val vurderinger = vilkåreneSegment.verdi
                vurderinger.isNotEmpty() && vurderinger
                    // Vi filtrerer bort vurderinger hvor noen vilkår er avslått, bortsett fra sykdomsvilkåret,
                    // som har unntak: om sykepengeerstatning er innvilget.
                    .filter { (vilkår, _) -> vilkår.type != Vilkårtype.SYKDOMSVILKÅRET }
                    .none { (_, vurdering) -> vurdering.utfall == Utfall.IKKE_OPPFYLT }
            }
            .mapValue { vilkårene ->
                prioriterVilkår(
                    vilkårene
                        .filter { (_, vurdering) -> vurdering.erVurdert() }
                        .map { (vilkår, vurdering) ->
                            Pair(
                                vilkår.type,
                                vurdering
                            )
                        }
                        .toSet()
                )
            }
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
    private fun prioriterVilkår(vilkårPar: Set<Pair<Vilkårtype, Vilkårsvurdering>>): RettighetsType {
        val (_, bistandsvurderingen) = vilkårPar.firstOrNull { it.first == Vilkårtype.BISTANDSVILKÅRET }
            ?: throw UgyldigForespørselException("Bistandsvilkåret må være til stede for å regne ut rettighetstype.")

        val (_, vilkårsVurdering) = requireNotNull(vilkårPar.firstOrNull { it.first == Vilkårtype.SYKDOMSVILKÅRET })
        val sykdomsUtfall = vilkårsVurdering.utfall

        // Hvis sykdomsvilkåret ikke er oppfylt, så kan man få rettighet om sykepengervilkåret er oppfylt
        if (sykdomsUtfall == Utfall.IKKE_OPPFYLT) {
            val sykepengervilkåret =
                vilkårPar.find { it.first == Vilkårtype.SYKEPENGEERSTATNING && it.second.utfall == Utfall.OPPFYLT }

            if (sykepengervilkåret != null) {
                return RettighetsType.SYKEPENGEERSTATNING
            }
        }

        // Vi har tatt hånd om sykepengervilkåret, og da må vi anta at 11-5 er oppfylt.
        require(sykdomsUtfall == Utfall.OPPFYLT) {
            "Sykepengeerstatning må være oppfylt om ikke 11-5 er oppfylt."
        }

        val bistandsvurderingInnvilgelsesårsak = bistandsvurderingen.innvilgelsesårsak

        // Gitt at 11-6 er innvilget, prioriter hvilket grunnlag det er gitt på.
        when (bistandsvurderingInnvilgelsesårsak) {
            Innvilgelsesårsak.STUDENT -> return RettighetsType.STUDENT
            Innvilgelsesårsak.ARBEIDSSØKER -> return RettighetsType.ARBEIDSSØKER
            Innvilgelsesårsak.VURDERES_FOR_UFØRETRYGD -> return RettighetsType.VURDERES_FOR_UFØRETRYGD
            Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG -> {}
            null -> {}
        }

        val sykepengervilkåret = vilkårPar.find { it.first == Vilkårtype.SYKEPENGEERSTATNING }

        if (sykepengervilkåret != null) {
            return RettighetsType.SYKEPENGEERSTATNING
        }

        // Med mindre noen annen innvilgelsesårsak er gitt, er grunntilfellet 11-6 (bistandsbehov).
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

private fun <T : Any> List<Tidslinje<T>>.outerJoin(): Tidslinje<List<T>> {
    return this.fold(Tidslinje()) { listeTidslinje, elementTidslinje ->
        listeTidslinje.outerJoin(elementTidslinje) { liste, element ->
            liste.orEmpty() + listOfNotNull(element)
        }
    }
}

