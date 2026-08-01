package no.nav.aap.behandlingsflyt.behandling.vilkår

import no.nav.aap.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.vilkårsresultat.Avslagsårsak
import no.nav.aap.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.vilkårsresultat.Utfall

class VurderingsResultat(
    val utfall: Utfall,
    val avslagsårsak: Avslagsårsak?,
    val innvilgelsesårsak: Innvilgelsesårsak?
) {
    fun versjon(): String {
        return ApplikasjonsVersjon.versjon
    }
}
