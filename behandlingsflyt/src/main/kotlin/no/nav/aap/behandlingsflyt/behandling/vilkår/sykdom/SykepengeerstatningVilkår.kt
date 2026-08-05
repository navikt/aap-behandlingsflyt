package no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import org.slf4j.LoggerFactory

object SykepengeerstatningVilkår :
    Vilkårsvurderer<SykepengerErstatningFaktagrunnlag> {
    private val log = LoggerFactory.getLogger(javaClass)
    override val vilkårtype: Vilkårtype = Vilkårtype.SYKEPENGEERSTATNING

    override fun vurder(faktagrunnlag: SykepengerErstatningFaktagrunnlag): Tidslinje<Vilkårsvurdering> {
        val sykdomsvurderingTidslinje = faktagrunnlag.sykdomGrunnlag?.somSykdomsvurderingstidslinje(
            maksDato = faktagrunnlag.rettighetsperiode.tom
        ).orEmpty()
        val yrkesskadevurderingTidslinje = faktagrunnlag.sykdomGrunnlag
            ?.yrkesskadevurdringTidslinje(faktagrunnlag.rettighetsperiode)
            .orEmpty()
        val sykepengeerstatningTidslinje = faktagrunnlag.sykepengeerstatningGrunnlag?.somTidslinje(
            kravDato = faktagrunnlag.rettighetsperiode.fom,
            sisteMuligDagMedYtelse = faktagrunnlag.rettighetsperiode.tom
        ).orEmpty()


        return Tidslinje.zip3(sykdomsvurderingTidslinje, sykepengeerstatningTidslinje, yrkesskadevurderingTidslinje)
            .mapValue { (sykdomsvurdering, sykepengeerstatningVurdering, yrkesskadevurdering) ->
                opprettVilkårsvurdering(
                    sykdomsvurdering,
                    sykepengeerstatningVurdering,
                    yrkesskadevurdering,
                    faktagrunnlag
                )
            }
    }

    private fun opprettVilkårsvurdering(
        sykdomsvurdering: Sykdomsvurdering?,
        sykepengeerstatningVurdering: SykepengerVurdering?,
        yrkesskadeVurdering: Yrkesskadevurdering?,
        grunnlag: SykepengerErstatningFaktagrunnlag,
    ): Vilkårsvurdering {
        if (sykdomsvurdering?.erKonsistentMedSykepengeerstatning(yrkesskadeVurdering) != sykdomsvurdering?.erKonsistentMedSykepengeerstatning(
                yrkesskadeVurdering
            )
        ) {
            log.error("Fant diff i sykepengeerstatningvilkår. Fortsetter med gammelt vilkår ")
        }

        return if (sykepengeerstatningVurdering?.harRettPå == true &&
            sykdomsvurdering?.erKonsistentMedSykepengeerstatning(yrkesskadeVurdering) ?: false
        ) {
            Vilkårsvurdering(
                Vilkårsperiode(
                    periode = grunnlag.rettighetsperiode,
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = sykepengeerstatningVurdering.begrunnelse,
                    innvilgelsesårsak = null,
                    avslagsårsak = null,
                    faktagrunnlag = grunnlag,
                )
            )
        } else {
            Vilkårsvurdering(
                Vilkårsperiode(
                    periode = grunnlag.rettighetsperiode,
                    utfall = Utfall.IKKE_OPPFYLT,
                    begrunnelse = sykepengeerstatningVurdering?.begrunnelse,
                    innvilgelsesårsak = null,
                    avslagsårsak = Avslagsårsak.IKKE_RETT_PA_SYKEPENGEERSTATNING,
                    faktagrunnlag = grunnlag,
                )
            )
        }
    }
}
