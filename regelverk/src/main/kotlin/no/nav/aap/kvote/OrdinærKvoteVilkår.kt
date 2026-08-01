package no.nav.aap.kvote

import no.nav.aap.rettighetstype.KvoteBruktOpp
import no.nav.aap.rettighetstype.KvoteOk
import no.nav.aap.vilkårsresultat.Avslagsårsak
import no.nav.aap.vilkårsresultat.Utfall
import no.nav.aap.vilkårsresultat.Vilkårsvurderer
import no.nav.aap.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Tidslinje

object OrdinærKvoteVilkår :
    Vilkårsvurderer<OrdinærKvoteFaktagrunnlag> {
    override val vilkårtype: Vilkårtype = Vilkårtype.ORDINÆR_KVOTE

    override fun vurder(faktagrunnlag: OrdinærKvoteFaktagrunnlag): Tidslinje<Vilkårsvurdering> {
        val (kvotevurderinger, _) = faktagrunnlag

        return kvotevurderinger.map { kvotevurdering ->
            when (kvotevurdering) {
                is KvoteOk if kvotevurdering.brukerKvote == Kvote.ORDINÆR -> {
                    Vilkårsvurdering(
                        utfall = Utfall.OPPFYLT,
                        manuellVurdering = false,
                        begrunnelse = null,
                        faktagrunnlag = faktagrunnlag,
                    )
                }

                is KvoteBruktOpp if kvotevurdering.kvoteBruktOpp == Kvote.ORDINÆR -> {
                    Vilkårsvurdering(
                        utfall = Utfall.IKKE_OPPFYLT,
                        manuellVurdering = false,
                        avslagsårsak = Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP,
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