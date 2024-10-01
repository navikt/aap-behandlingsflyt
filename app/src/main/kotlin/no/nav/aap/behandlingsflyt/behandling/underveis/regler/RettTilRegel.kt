package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje

/**
 * Setter opp tidslinjen hvor bruker har grunnleggende rett til ytelsen
 *
 * - Alder (18 - 67)
 * - Perioder med ytelse (Sykdom, Bistand, Sykepengeerstatning, Student)
 *
 */
class RettTilRegel : UnderveisRegel {
    override fun vurder(
        input: UnderveisInput,
        resultat: Tidslinje<Vurdering>
    ): Tidslinje<Vurdering> {
        require(input.relevanteVilkår.any { it.type == Vilkårtype.ALDERSVILKÅRET })
        require(input.relevanteVilkår.any { it.type == Vilkårtype.BISTANDSVILKÅRET })
        require(input.relevanteVilkår.any { it.type == Vilkårtype.MEDLEMSKAP })
        require(input.relevanteVilkår.any { it.type == Vilkårtype.SYKDOMSVILKÅRET })

        return input.relevanteVilkår.fold(resultat) { retur, vilkår ->
            val segmenter = vilkår.vilkårsperioder()
                .map { Segment(it.periode, EnkelVurdering(vilkår.type, it.utfall)) }

            retur.kombiner(Tidslinje(segmenter), JoinStyle.OUTER_JOIN { periode, venstre, høyre ->
                SlåSammenVurderingerSammenslåer().sammenslå(periode, venstre, høyre)
            })
        }
    }
}