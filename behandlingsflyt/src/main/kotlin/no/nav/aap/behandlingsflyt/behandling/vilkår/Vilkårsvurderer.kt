package no.nav.aap.behandlingsflyt.behandling.vilkår

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag

interface Vilkårsvurderer<T : Faktagrunnlag> {

    fun vurder(grunnlag: T)
}
