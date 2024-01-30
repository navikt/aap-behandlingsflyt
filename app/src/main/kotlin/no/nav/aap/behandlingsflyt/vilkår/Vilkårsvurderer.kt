package no.nav.aap.behandlingsflyt.vilkår

import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Faktagrunnlag

interface Vilkårsvurderer<T : Faktagrunnlag> {

    fun vurder(grunnlag: T): VurderingsResultat
}
