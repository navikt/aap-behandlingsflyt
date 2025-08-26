package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.outerJoinNotNull

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
        require(vilkår.any { it.type == Vilkårtype.ALDERSVILKÅRET }) { "Aldersvilkåret må være vurdert." }
        require(vilkår.any { it.type == Vilkårtype.BISTANDSVILKÅRET }) { "Bistandsvilkåret må være vurdert." }
        require(vilkår.any { it.type == Vilkårtype.MEDLEMSKAP }) { "Medlemskap må være vurdert." }
        require(vilkår.any { it.type == Vilkårtype.LOVVALG }) { "Lovvalg må være vurdert."}
        require(vilkår.any { it.type == Vilkårtype.SYKDOMSVILKÅRET }) { "Sykdomsvilkåret må være vurdert." }
        require(vilkår.distinctBy { it.type }.size == vilkår.size)

        return vilkår
            .map { vilkår -> vilkår.tidslinje().mapValue { vurdering -> vilkår to vurdering } }
            .outerJoinNotNull { vurderinger ->
                if (vurderinger.isEmpty()) {
                    return@outerJoinNotNull null
                }

                // Vi filtrerer bort vurderinger hvor noen vilkår er avslått
                val harVilkårIkkeOppfylt = vurderinger.any { (_, vurdering) ->
                    vurdering.utfall == Utfall.IKKE_OPPFYLT
                }
                if (harVilkårIkkeOppfylt) {
                    return@outerJoinNotNull null
                }

                // Bistandsvilkåret kan være merket som ikke relevant ved sykepengeerstatning
                val bistandsvilkåretErIkkeOppfylt = vurderinger.none { (vilkår, vurdering) ->
                    vilkår.type == Vilkårtype.BISTANDSVILKÅRET && (vurdering.erOppfylt() || vurdering.erIkkeRelevant())
                }
                if (bistandsvilkåretErIkkeOppfylt) {
                    return@outerJoinNotNull null
                }

                val sykdomsvilkåretErIkkeVurdert = vurderinger.none { (vilkår, vurdering) ->
                    vilkår.type == Vilkårtype.SYKDOMSVILKÅRET && vurdering.erVurdert()
                }

                if (sykdomsvilkåretErIkkeVurdert) {
                    return@outerJoinNotNull null
                }

                prioriterVilkår(
                    vurderinger
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

        // Hvis bistandsvurderingen ikke er relevant, kan det være fordi det er sykepengeerstatning
        if (bistandsvurderingen.erIkkeRelevant()) {
            val sykepengerErstatning =
                vilkårPar.find {
                    it.first == Vilkårtype.SYKDOMSVILKÅRET
                            && it.second.utfall == Utfall.OPPFYLT
                            && it.second.innvilgelsesårsak == Innvilgelsesårsak.SYKEPENGEERSTATNING
                }

            if (sykepengerErstatning != null) {
                return RettighetsType.SYKEPENGEERSTATNING
            }

        }

        // Vi har tatt hånd om sykepengervilkåret, og da må vi anta at 11-5 er oppfylt.
        require(sykdomsUtfall == Utfall.OPPFYLT) {
            "Sykepengeerstatning må være oppfylt om ikke 11-5 er oppfylt."
        }

        // Sjekker på overgang uføre og overgang arbeid før bistandsvurderingen
        val harOppfyltVilkårForOvergangUføre =
            vilkårPar.any {
                it.first == Vilkårtype.OVERGANGUFØREVILKÅRET
                        && it.second.utfall == Utfall.OPPFYLT
                        && it.second.innvilgelsesårsak == Innvilgelsesårsak.VURDERES_FOR_UFØRETRYGD
            }
        if (harOppfyltVilkårForOvergangUføre) {
            return RettighetsType.VURDERES_FOR_UFØRETRYGD
        }
        val harOppfyltVilkårForOvergangArbeid =
            vilkårPar.any {
                it.first == Vilkårtype.OVERGANGARBEIDVILKÅRET
                        && it.second.utfall == Utfall.OPPFYLT
                        && it.second.innvilgelsesårsak == Innvilgelsesårsak.VURDERES_FOR_UFØRETRYGD
            }
        if (harOppfyltVilkårForOvergangArbeid) {
            return RettighetsType.ARBEIDSSØKER
        }

        val bistandsvurderingInnvilgelsesårsak = bistandsvurderingen.innvilgelsesårsak

        // Gitt at 11-6 er innvilget, prioriter hvilket grunnlag det er gitt på.
        when (bistandsvurderingInnvilgelsesårsak) {
            Innvilgelsesårsak.STUDENT -> return RettighetsType.STUDENT
            Innvilgelsesårsak.ARBEIDSSØKER -> return RettighetsType.ARBEIDSSØKER
            Innvilgelsesårsak.VURDERES_FOR_UFØRETRYGD -> return RettighetsType.VURDERES_FOR_UFØRETRYGD
            Innvilgelsesårsak.SYKEPENGEERSTATNING -> return RettighetsType.SYKEPENGEERSTATNING
            Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG -> {}
            null -> {}
        }

        // Med mindre noen annen innvilgelsesårsak er gitt, er grunntilfellet 11-6 (bistandsbehov).
        return RettighetsType.BISTANDSBEHOV
    }

    fun tidslinjeFor(vilkårstype: Vilkårtype): Tidslinje<Vilkårsvurdering> {
        return optionalVilkår(vilkårstype)?.tidslinje() ?: Tidslinje()
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