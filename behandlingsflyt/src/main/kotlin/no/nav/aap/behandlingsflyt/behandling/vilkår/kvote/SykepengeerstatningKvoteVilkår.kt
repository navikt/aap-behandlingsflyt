package no.nav.aap.behandlingsflyt.behandling.vilkår.kvote

import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteBruktOpp
import no.nav.aap.behandlingsflyt.behandling.rettighetstype.KvoteOk
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype

class SykepengeerstatningKvoteVilkår(vilkårsresultat: Vilkårsresultat) :
    Vilkårsvurderer<SykepengeerstatningKvoteFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING_KVOTE)

    override fun vurder(grunnlag: SykepengeerstatningKvoteFaktagrunnlag) {
        val (kvotevurderinger, _) = grunnlag

        vilkår.leggTilVurderinger(
            kvotevurderinger.map { kvotevurdering ->
                when (kvotevurdering) {
                    is KvoteOk if kvotevurdering.brukerKvote == Kvote.SYKEPENGEERSTATNING -> {
                        Vilkårsvurdering(
                            utfall = Utfall.OPPFYLT,
                            manuellVurdering = false,
                            begrunnelse = null,
                            faktagrunnlag = grunnlag,
                        )
                    }

                    is KvoteBruktOpp if kvotevurdering.kvoteBruktOpp == Kvote.SYKEPENGEERSTATNING -> {
                        Vilkårsvurdering(
                            utfall = Utfall.IKKE_OPPFYLT,
                            manuellVurdering = false,
                            avslagsårsak = Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP,
                            begrunnelse = null,
                            faktagrunnlag = grunnlag,
                        )
                    }

                    else -> {
                        Vilkårsvurdering(
                            utfall = Utfall.IKKE_RELEVANT,
                            manuellVurdering = false,
                            begrunnelse = null,
                            faktagrunnlag = grunnlag,
                        )
                    }
                }
            })

    }
}