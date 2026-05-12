package no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import org.slf4j.LoggerFactory

class SykepengeerstatningVilkår(vilkårsresultat: Vilkårsresultat) :
    Vilkårsvurderer<SykepengerErstatningFaktagrunnlag> {
    private val log = LoggerFactory.getLogger(javaClass)
    private val vilkår: Vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING)

    override fun vurder(grunnlag: SykepengerErstatningFaktagrunnlag) {
        val sykdomsvurderingTidslinje = grunnlag.sykdomGrunnlag?.somSykdomsvurderingstidslinje(
            maksDato = grunnlag.rettighetsperiode.tom
        ).orEmpty()
        val yrkesskadevurderingTidslinje = grunnlag.sykdomGrunnlag
            ?.yrkesskadevurdringTidslinje(grunnlag.rettighetsperiode)
            .orEmpty()
        val sykepengeerstatningTidslinje = grunnlag.sykepengeerstatningGrunnlag?.somTidslinje(
            kravDato = grunnlag.rettighetsperiode.fom,
            sisteMuligDagMedYtelse = grunnlag.rettighetsperiode.tom
        ).orEmpty()

        val tidslinje =
            Tidslinje.zip3(sykdomsvurderingTidslinje, sykepengeerstatningTidslinje, yrkesskadevurderingTidslinje)
                .mapValue { (sykdomsvurdering, sykepengeerstatningVurdering, yrkesskadevurdering) ->
                    opprettVilkårsvurdering(
                        sykdomsvurdering,
                        sykepengeerstatningVurdering,
                        yrkesskadevurdering,
                        grunnlag
                    )
                }
        vilkår.leggTilVurderinger(tidslinje)
    }

    private fun opprettVilkårsvurdering(
        sykdomsvurdering: Sykdomsvurdering?,
        sykepengeerstatningVurdering: SykepengerVurdering?,
        yrkesskadeVurdering: Yrkesskadevurdering?,
        grunnlag: SykepengerErstatningFaktagrunnlag,
    ): Vilkårsvurdering {
        if (sykdomsvurdering?.erKonsistentMedSykepengeerstatning(yrkesskadeVurdering) != sykdomsvurdering?.erKonsistentMedSykepengeerstatning(yrkesskadeVurdering)) {
            log.error("Fant diff i sykepengeerstatningvilkår. Fortsetter med gammelt vilkår ")
        }
        
        return if (sykepengeerstatningVurdering?.harRettPå == true &&
            sykdomsvurdering?.erKonsistentMedSykepengeerstatning(yrkesskadeVurdering) ?: false // TODO: Bytt ut denne med ny
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
