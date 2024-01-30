package no.nav.aap.behandlingsflyt.vilkår.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.vilkår.VurderingsResultat
import no.nav.aap.verdityper.Periode

class Bistandsvilkåret(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<BistandFaktagrunnlag> {
    private val vilkår: Vilkår

    init {
        this.vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)
    }

    override fun vurder(grunnlag: BistandFaktagrunnlag): VurderingsResultat {
        val utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null
        var innvilgelsesårsak: Innvilgelsesårsak? = null

        if (grunnlag.studentvurdering?.oppfyller11_14 == true) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.STUDENT
        } else if (grunnlag.vurdering?.erBehovForBistand == true) {
            utfall = Utfall.OPPFYLT
        } else {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON // TODO: Må ha mer
        }

        return lagre(
            grunnlag, VurderingsResultat(
                utfall = utfall,
                avslagsårsak = avslagsårsak,
                innvilgelsesårsak = innvilgelsesårsak
            )
        )
    }

    private fun lagre(grunnlag: BistandFaktagrunnlag, vurderingsResultat: VurderingsResultat): VurderingsResultat {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                Periode(grunnlag.vurderingsdato, grunnlag.sisteDagMedMuligYtelse),
                vurderingsResultat.utfall,
                false,
                null,
                vurderingsResultat.innvilgelsesårsak,
                vurderingsResultat.avslagsårsak,
                grunnlag,
                vurderingsResultat.versjon()
            )
        )

        return vurderingsResultat
    }

}
