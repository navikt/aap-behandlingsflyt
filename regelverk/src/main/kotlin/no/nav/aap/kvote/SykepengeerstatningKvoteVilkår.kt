package no.nav.aap.kvote

import no.nav.aap.rettighetstype.KvoteBruktOpp
import no.nav.aap.rettighetstype.KvoteOk
import no.nav.aap.vilkårsresultat.Avslagsårsak
import no.nav.aap.vilkårsresultat.Utfall
import no.nav.aap.vilkårsresultat.Vilkårsvurderer
import no.nav.aap.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Tidslinje

object SykepengeerstatningKvoteVilkår :
    Vilkårsvurderer<SykepengeerstatningKvoteFaktagrunnlag> {
    override val vilkårtype: Vilkårtype = Vilkårtype.SYKEPENGEERSTATNING_KVOTE

    override fun vurder(faktagrunnlag: SykepengeerstatningKvoteFaktagrunnlag): Tidslinje<Vilkårsvurdering> {
        val (kvotevurderinger, _) = faktagrunnlag

        return kvotevurderinger.map { kvotevurdering ->
            when (kvotevurdering) {
                is KvoteOk if kvotevurdering.brukerKvote == Kvote.SYKEPENGEERSTATNING -> {
                    Vilkårsvurdering(
                        utfall = Utfall.OPPFYLT,
                        manuellVurdering = false,
                        begrunnelse = null,
                        faktagrunnlag = faktagrunnlag,
                    )
                }

                is KvoteBruktOpp if kvotevurdering.kvoteBruktOpp == Kvote.SYKEPENGEERSTATNING -> {
                    Vilkårsvurdering(
                        utfall = Utfall.IKKE_OPPFYLT,
                        manuellVurdering = false,
                        avslagsårsak = Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP,
                        begrunnelse = null,
                        faktagrunnlag = faktagrunnlag,
                    )
                }

                else -> {
                    Vilkårsvurdering(
                        utfall = Utfall.IKKE_RELEVANT,
                        manuellVurdering = false,
                        begrunnelse = null,
                        faktagrunnlag = faktagrunnlag,
                    )
                }
            }
        }
    }
}