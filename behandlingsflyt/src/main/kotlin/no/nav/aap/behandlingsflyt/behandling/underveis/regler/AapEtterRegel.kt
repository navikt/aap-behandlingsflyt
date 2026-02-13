package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.vurderRettighetstypeOgKvoter
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Tidslinje

class AapEtterRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        if (input.rettighetstypeGrunnlag != null) {
            val rettighetstypeTidslinje = input.rettighetstypeGrunnlag.rettighetstypeTidslinje
            val varighetTidslinje = byggVarighetstidslinjeFraKvotevilkår(input)

            return resultat
                .leggTilVurderinger(rettighetstypeTidslinje, Vurdering::leggTilRettighetstype)
                .leggTilVurderinger(varighetTidslinje, Vurdering::leggTilVarighetVurdering)

        } else {
            val kvoteOgRettighetstype = vurderRettighetstypeOgKvoter(input.vilkårsresultat, input.kvoter)
            return resultat
                .leggTilVurderinger(
                    kvoteOgRettighetstype.mapNotNull { it.rettighetsType },
                    Vurdering::leggTilRettighetstype
                )
                .leggTilVurderinger(kvoteOgRettighetstype, Vurdering::leggTilVarighetVurdering)
        }
    }

    private fun byggVarighetstidslinjeFraKvotevilkår(input: UnderveisInput): Tidslinje<VarighetVurdering> {
        val ordinærKvote = input.vilkårsresultat.finnVilkår(Vilkårtype.ORDINÆR_KVOTE).tidslinje()
        val speKvote = input.vilkårsresultat.finnVilkår(Vilkårtype.SYKEPENGEERSTATNING_KVOTE).tidslinje()

        return ordinærKvote.outerJoin(speKvote) { ordinær, spe ->

            when {
                ordinær?.utfall == Utfall.OPPFYLT -> Oppfylt(brukerAvKvoter = setOf(Kvote.ORDINÆR))
                spe?.utfall == Utfall.OPPFYLT -> Oppfylt(brukerAvKvoter = setOf(Kvote.SYKEPENGEERSTATNING))
                ordinær?.utfall == Utfall.IKKE_OPPFYLT -> Avslag(
                    brukerAvKvoter = setOf(Kvote.ORDINÆR), avslagsårsaker = setOf(
                        VarighetVurdering.Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP
                    )
                )

                spe?.utfall == Utfall.IKKE_OPPFYLT -> Avslag(
                    brukerAvKvoter = setOf(Kvote.SYKEPENGEERSTATNING), avslagsårsaker = setOf(
                        VarighetVurdering.Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP
                    )
                )

                else -> Oppfylt(brukerAvKvoter = emptySet())
            }

        }
    }
}
