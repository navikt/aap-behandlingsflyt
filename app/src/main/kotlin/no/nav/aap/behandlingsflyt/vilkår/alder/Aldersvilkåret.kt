package no.nav.aap.behandlingsflyt.vilkår.alder

import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.vilkår.VurderingsResultat

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
            utfall = Utfall.IKKE_OPPFYLT
            avslagsårsak = Avslagsårsak.BRUKER_UNDER_18
        } else if (alderPåSøknadsdato >= 67) {
            utfall = Utfall.IKKE_OPPFYLT
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
