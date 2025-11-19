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
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty

class SykepengeerstatningVilkår(vilkårsresultat: Vilkårsresultat) :
    Vilkårsvurderer<SykepengerErstatningFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKEPENGEERSTATNING)

    override fun vurder(grunnlag: SykepengerErstatningFaktagrunnlag) {
        val sykdomsvurderingTidslinje = grunnlag.sykdomGrunnlag?.somSykdomsvurderingstidslinje(
            maksDato = grunnlag.rettighetsperiode.tom
        ).orEmpty()
        val sykepengeerstatningTidslinje = grunnlag.sykepengeerstatningGrunnlag?.somTidslinje(
            kravDato = grunnlag.rettighetsperiode.fom,
            sisteMuligDagMedYtelse = grunnlag.rettighetsperiode.tom
        ).orEmpty()

        val tidslinje = Tidslinje.zip2(sykdomsvurderingTidslinje, sykepengeerstatningTidslinje)
            .mapValue { (sykdomsvurdering, sykepengeerstatningVurdering) ->
                opprettVilkårsvurdering(sykdomsvurdering, sykepengeerstatningVurdering, grunnlag)
            }
        vilkår.leggTilVurderinger(tidslinje)
    }

    private fun opprettVilkårsvurdering(
        sykdomsvurdering: Sykdomsvurdering?,
        sykepengeerstatningVurdering: SykepengerVurdering?,
        grunnlag: SykepengerErstatningFaktagrunnlag,
    ): Vilkårsvurdering {
        return if (sykepengeerstatningVurdering?.harRettPå == true && sykdomsvurdering?.erOppfyltSettBortIfraVissVarighet() == true) {
            Vilkårsvurdering(
                Vilkårsperiode(
                    periode = grunnlag.rettighetsperiode,
                    utfall = Utfall.OPPFYLT,
                    begrunnelse = null,
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
                    begrunnelse = null,
                    innvilgelsesårsak = null,
                    avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON, // TODO noe mer rett
                    faktagrunnlag = grunnlag,
                )
            )
        }
    }

//
//    override fun vurder(grunnlag: SykepengerErstatningFaktagrunnlag) {
//        val utfall: Utfall
//        var avslagsårsak: Avslagsårsak? = null
//
//        val sykdomsvurdering = grunnlag.vurdering
//
//        // TODO: er dette nok vurdering?
//
//        if (sykepengerVurdering?.harRettPå == true && sykdomVurdering?.erOppfyltSettBortIfraVissVarighet() == true) {
//            utfall = Utfall.OPPFYLT
//            innvilgelsesårsak = Innvilgelsesårsak.SYKEPENGEERSTATNING
//        
//        if (sykdomsvurdering.harRettPå == true) {
//            utfall = Utfall.OPPFYLT
//        } else {
//            utfall = Utfall.IKKE_OPPFYLT
//            avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON // TODO noe mer rett
//        }
//
//        lagre(
//            grunnlag, VurderingsResultat(
//                utfall = utfall,
//                avslagsårsak,
//                null
//            )
//        )
//    }
//
//    private fun lagre(
//        grunnlag: SykepengerErstatningFaktagrunnlag,
//        vurderingsResultat: VurderingsResultat
//    ): VurderingsResultat {
//        vilkår.leggTilVurdering(
//            Vilkårsperiode(
//                Periode(grunnlag.vurderingsdato, grunnlag.sisteDagMedMuligYtelse),
//                vurderingsResultat.utfall,
//                false,
//                null,
//                null,
//                vurderingsResultat.avslagsårsak,
//                grunnlag,
//                vurderingsResultat.versjon()
//            )
//        )
//
//        return vurderingsResultat
//    }

}
