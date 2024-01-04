package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.tidslinje.Segment
import no.nav.aap.behandlingsflyt.tidslinje.Tidslinje

/**
 * Setter opp tidslinjen hvor bruker har grunnleggende rett til ytelsen
 *
 * - Alder (18 - 67)
 * - Perioder med ytelse (Sykdom, Bistand, Sykepengeerstatning, Student)
 * - Varigheten (11-12)
 *   - 3 år per "krav"
 *
 */
class RettTilRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        require(input.relevanteVilkår.any { it.type == Vilkårtype.ALDERSVILKÅRET })
        require(input.relevanteVilkår.any { it.type == Vilkårtype.BISTANDSVILKÅRET })
        require(input.relevanteVilkår.any { it.type == Vilkårtype.SYKDOMSVILKÅRET })

        var retur = resultat
        input.relevanteVilkår.forEach { vilkår ->
            val segmenter =
                vilkår.vilkårsperioder().map { Segment(it.periode, EnkelVurdering(vilkår.type, it.utfall)) }
            retur = retur.kombiner(Tidslinje(segmenter), SlåSammenVurderingerSammenslåer())
        }
        return retur
    }
}