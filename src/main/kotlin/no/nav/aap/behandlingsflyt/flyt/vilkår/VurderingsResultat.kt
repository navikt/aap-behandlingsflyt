package no.nav.aap.behandlingsflyt.flyt.vilkår

class VurderingsResultat(
    val utfall: Utfall,
    val avslagsårsak: Avslagsårsak?,
    val innvilgelsesårsak: Innvilgelsesårsak?
) {
    fun versjon(): String {
        return ApplikasjonsVersjon.versjon
    }
}
