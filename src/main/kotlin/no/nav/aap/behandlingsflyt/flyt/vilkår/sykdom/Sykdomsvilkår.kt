package no.nav.aap.behandlingsflyt.flyt.vilkår.sykdom

import no.nav.aap.behandlingsflyt.domene.Periode
import no.nav.aap.behandlingsflyt.flyt.vilkår.Avslagsårsak
import no.nav.aap.behandlingsflyt.flyt.vilkår.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.flyt.vilkår.TomtBeslutningstre
import no.nav.aap.behandlingsflyt.flyt.vilkår.Utfall
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkår
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsperiode
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsresultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.vilkår.VurderingsResultat

class Sykdomsvilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<SykdomsFaktagrunnlag> {
    private val vilkår: Vilkår

    init {
        this.vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
    }

    override fun vurder(grunnlag: SykdomsFaktagrunnlag): VurderingsResultat {
        val utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null
        var innvilgelsesårsak: Innvilgelsesårsak? = null

        val sykdomsvurdering = grunnlag.sykdomsvurdering

        if (sykdomsvurdering.erSkadeSykdomEllerLyteVesentligdel && sykdomsvurdering.erNedsettelseIArbeidsevneHøyereEnnNedreGrense == true) {
            utfall = Utfall.OPPFYLT
            val yrkesskadevurdering = grunnlag.yrkesskadevurdering
            if (yrkesskadevurdering != null && yrkesskadevurdering.erÅrsakssammenheng) {
                innvilgelsesårsak = Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG
            }
        } else {
            utfall = Utfall.IKKE_OPPFYLT
            if (!sykdomsvurdering.erSkadeSykdomEllerLyteVesentligdel) {

            } else if (sykdomsvurdering.erNedsettelseIArbeidsevneHøyereEnnNedreGrense == false) {

            } else {
                avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON // TODO noe mer rett
            }
        }

        return lagre(
            grunnlag,
            VurderingsResultat(
                utfall = utfall,
                avslagsårsak = avslagsårsak,
                innvilgelsesårsak = innvilgelsesårsak,
                beslutningstre = TomtBeslutningstre()
            )
        )
    }

    private fun lagre(grunnlag: SykdomsFaktagrunnlag, vurderingsResultat: VurderingsResultat): VurderingsResultat {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                Periode(grunnlag.vurderingsdato, grunnlag.sisteDagMedMuligYtelse),
                vurderingsResultat.utfall,
                false,
                null,
                vurderingsResultat.innvilgelsesårsak,
                vurderingsResultat.avslagsårsak,
                grunnlag,
                vurderingsResultat.beslutningstre
            )
        )

        return vurderingsResultat
    }

}
