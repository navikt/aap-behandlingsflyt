package no.nav.aap.behandlingsflyt.behandling.vilkår.kvote

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteBruktOpp
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteOk
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
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