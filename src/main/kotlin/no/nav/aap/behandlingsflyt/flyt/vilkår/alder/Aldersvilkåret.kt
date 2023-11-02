package no.nav.aap.behandlingsflyt.flyt.vilkår.alder

import no.nav.aap.behandlingsflyt.flyt.vilkår.Avslagsårsak
import no.nav.aap.behandlingsflyt.flyt.vilkår.Utfall
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkår
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsperiode
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsresultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import no.nav.aap.behandlingsflyt.flyt.vilkår.VurderingsResultat

class Aldersvilkåret(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<Aldersgrunnlag> {
    private val vilkår: Vilkår

    init {
        this.vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.ALDERSVILKÅRET)
    }

    // TODO Det må avklares hva som er riktig adferd dersom bruker søker før fylte 18
    override fun vurder(grunnlag: Aldersgrunnlag): VurderingsResultat {
        val utfall: Utfall
        var avslagsårsak: Avslagsårsak? = null

        val alderPåSøknadsdato = grunnlag.alderPåSøknadsdato()

        if (alderPåSøknadsdato < 18) {
            utfall = Utfall.IKKE_OPPFYLT // TODO: Årsak + hjemmel ?
            avslagsårsak = Avslagsårsak.BRUKER_UNDER_18
        } else if (alderPåSøknadsdato >= 67) {
            utfall = Utfall.IKKE_OPPFYLT // TODO: Årsak + hjemmel ?
            avslagsårsak = Avslagsårsak.BRUKER_OVER_67
        } else {
            utfall = Utfall.OPPFYLT
        }

        return lagre(
            grunnlag, VurderingsResultat(
                utfall = utfall,
                avslagsårsak = avslagsårsak,
                innvilgelsesårsak = null
            )
        )
    }

    private fun lagre(grunnlag: Aldersgrunnlag, vurderingsResultat: VurderingsResultat): VurderingsResultat {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                periode = grunnlag.periode,
                utfall = vurderingsResultat.utfall,
                avslagsårsak = vurderingsResultat.avslagsårsak,
                begrunnelse = null,
                faktagrunnlag = grunnlag,
                versjon = vurderingsResultat.versjon()
            )
        )

        return vurderingsResultat
    }

}
