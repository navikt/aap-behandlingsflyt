package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak

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