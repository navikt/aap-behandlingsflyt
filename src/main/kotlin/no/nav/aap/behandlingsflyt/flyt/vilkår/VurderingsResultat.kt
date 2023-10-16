package no.nav.aap.behandlingsflyt.flyt.vilkår

class VurderingsResultat(
    val utfall: Utfall,
    val avslagsårsak: Avslagsårsak?,
    val beslutningstre: Beslutningstre,
    val innvilgelsesårsak: Innvilgelsesårsak?
)
