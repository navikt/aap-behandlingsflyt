package no.nav.aap.vilkår.alder

import no.nav.aap.domene.behandling.TomtBeslutningstre
import no.nav.aap.domene.behandling.Utfall
import no.nav.aap.vilkår.Vilkårsvurderer
import no.nav.aap.vilkår.VurderingsResultat

object Aldersvilkåret : Vilkårsvurderer<Aldersgrunnlag> {
    // TODO Det må avklares hva som er riktig adferd dersom bruker søker før fylte 18
    override fun vurder(grunnlag: Aldersgrunnlag): VurderingsResultat {
        val alderPåSøknadsdato = grunnlag.alderPåSøknadsdato()
        val resultat = if (alderPåSøknadsdato < 18) {
            Utfall.IKKE_OPPFYLT // TODO: Årsak + hjemmel ?
        } else {
            if (alderPåSøknadsdato >= 67) {
                Utfall.IKKE_OPPFYLT // TODO: Årsak + hjemmel ?
            } else {
                Utfall.OPPFYLT
            }
        }

        return VurderingsResultat(resultat, TomtBeslutningstre())
    }

}
