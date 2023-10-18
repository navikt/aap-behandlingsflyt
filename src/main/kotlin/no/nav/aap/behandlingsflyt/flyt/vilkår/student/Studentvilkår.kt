package no.nav.aap.behandlingsflyt.flyt.vilkår.student

import no.nav.aap.behandlingsflyt.domene.Periode
import no.nav.aap.behandlingsflyt.flyt.vilkår.TomtBeslutningstre
import no.nav.aap.behandlingsflyt.flyt.vilkår.Utfall
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkår
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsperiode
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsresultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.vilkår.VurderingsResultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.Avslagsårsak
import no.nav.aap.behandlingsflyt.flyt.vilkår.Innvilgelsesårsak

class Studentvilkår(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<StudentFaktagrunnlag> {
    private val vilkår: Vilkår

    init {
        this.vilkår = vilkårsresultat.finnVilkår(Vilkårtype.STUDENTVILKÅRET)
    }

    override fun vurder(grunnlag: StudentFaktagrunnlag): VurderingsResultat {
        val utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null
        var innvilgelsesårsak: Innvilgelsesårsak? = null

        val studentvurdering = grunnlag.studentvurdering

        if (studentvurdering.oppfyller11_14 == true && studentvurdering.oppfyller7 == true) {
            utfall = Utfall.OPPFYLT
        } else {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON // TODO noe mer rett
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

    private fun lagre(grunnlag: StudentFaktagrunnlag, vurderingsResultat: VurderingsResultat): VurderingsResultat {
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