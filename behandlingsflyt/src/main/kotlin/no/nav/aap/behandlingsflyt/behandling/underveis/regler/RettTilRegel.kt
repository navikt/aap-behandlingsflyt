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
        val relevanteVilkår = input.vilkårsresultat.alle().filter { it.type.obligatorisk }
        require(relevanteVilkår.any { it.type == Vilkårtype.ALDERSVILKÅRET })
        require(relevanteVilkår.any { it.type == Vilkårtype.BISTANDSVILKÅRET })
        require(relevanteVilkår.any { it.type == Vilkårtype.MEDLEMSKAP })
        require(relevanteVilkår.any { it.type == Vilkårtype.LOVVALG })
        require(relevanteVilkår.any { it.type == Vilkårtype.SYKDOMSVILKÅRET })
        require(relevanteVilkår.distinctBy { it.type }.size == relevanteVilkår.size)

        return relevanteVilkår.fold(resultat) { retur, vilkår ->
            val segmenter = vilkår.vilkårsperioder()
                .map { Segment(it.periode, VilkårVurdering(vilkår.type, it.utfall, it.innvilgelsesårsak)) }

            retur.leggTilVurderinger(Tidslinje(segmenter), Vurdering::leggTilVurdering)
        }
    }
}