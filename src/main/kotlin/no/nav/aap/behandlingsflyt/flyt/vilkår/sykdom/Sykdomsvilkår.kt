package no.nav.aap.behandlingsflyt.flyt.vilkår.sykdom

import no.nav.aap.behandlingsflyt.domene.Periode
import no.nav.aap.behandlingsflyt.flyt.vilkår.Avslagsårsak
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

        val sykdomsvurdering = grunnlag.sykdomsvurdering

        if (sykdomsvurdering.erSkadeSykdomEllerLyteVesentligdel && sykdomsvurdering.erNedsettelseIArbeidsevneHøyereEnnNedreGrense == true) {
            utfall = Utfall.OPPFYLT
            // TODO: Legge til innvilget etter (f.eks YS)
        } else {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON // TODO noe mer rett
        }

        return lagre(grunnlag, VurderingsResultat(utfall = utfall, avslagsårsak, TomtBeslutningstre()))
    }

    private fun lagre(grunnlag: SykdomsFaktagrunnlag, vurderingsResultat: VurderingsResultat): VurderingsResultat {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                Periode(grunnlag.vurderingsdato, grunnlag.sisteDagMedMuligYtelse),
                vurderingsResultat.utfall,
                false,
                null,
                null,
                vurderingsResultat.avslagsårsak,
                grunnlag,
                vurderingsResultat.beslutningstre
            )
        )

        return vurderingsResultat
    }

}
