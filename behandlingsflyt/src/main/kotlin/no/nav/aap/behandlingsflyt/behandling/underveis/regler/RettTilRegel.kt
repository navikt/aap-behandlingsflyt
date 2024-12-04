package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje

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
        require(input.relevanteVilkår.distinctBy { it.type } == input.relevanteVilkår)

        return input.relevanteVilkår.fold(resultat) { retur, vilkår ->
            val segmenter = vilkår.vilkårsperioder()
                .map { Segment(it.periode, EnkelVurdering(vilkår.type, it.utfall, it.innvilgelsesårsak)) }

            retur.leggTilVurderinger(Tidslinje(segmenter), Vurdering::leggTilVurdering)
        }
    }
}