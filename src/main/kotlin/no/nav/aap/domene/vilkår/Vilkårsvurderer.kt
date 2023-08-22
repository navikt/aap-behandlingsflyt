package no.nav.aap.domene.vilkår

import no.nav.aap.domene.behandling.Faktagrunnlag

interface Vilkårsvurderer<T : Faktagrunnlag> {

    fun vurder(grunnlag: T): VurderingsResultat
}
