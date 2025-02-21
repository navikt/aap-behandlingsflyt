package no.nav.aap.behandlingsflyt.behandling.vilkår.bistand

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.behandling.vilkår.VurderingsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.type.Periode

class Bistandsvilkåret(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<BistandFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

    override fun vurder(grunnlag: BistandFaktagrunnlag) {
        val utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null
        var innvilgelsesårsak: Innvilgelsesårsak? = null
        val studentVurdering = grunnlag.studentvurdering

        if (studentVurdering?.erOppfylt() == true) {
            utfall = Utfall.OPPFYLT
            innvilgelsesårsak = Innvilgelsesårsak.STUDENT
        } else if (grunnlag.vurdering?.erBehovForBistand() == true) {
            utfall = Utfall.OPPFYLT
        } else {
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.MANGLENDE_DOKUMENTASJON // TODO: Må ha mer
        }
        // TODO!!!
        // 11-18 - under behandling av krav om uføretrygd
        // 11-17 - i perioden som arbeidssøker
        // VIKTIG

        lagre(
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
