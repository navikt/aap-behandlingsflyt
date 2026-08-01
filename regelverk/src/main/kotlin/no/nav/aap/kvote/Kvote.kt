package no.nav.aap.kvote

import no.nav.aap.vilkårsresultat.Avslagsårsak

enum class Kvote(
    val avslagsårsak: Avslagsårsak,
) {
    ORDINÆR(
        avslagsårsak = Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP,
    ),
    SYKEPENGEERSTATNING(
        avslagsårsak = Avslagsårsak.SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP,
    ),
}