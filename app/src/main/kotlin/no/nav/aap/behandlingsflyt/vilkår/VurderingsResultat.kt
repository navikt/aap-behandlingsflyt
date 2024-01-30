package no.nav.aap.behandlingsflyt.vilkår

import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Utfall

class VurderingsResultat(
    val utfall: Utfall,
    val avslagsårsak: Avslagsårsak?,
    val innvilgelsesårsak: Innvilgelsesårsak?
) {
    fun versjon(): String {
        return ApplikasjonsVersjon.versjon
    }
}
