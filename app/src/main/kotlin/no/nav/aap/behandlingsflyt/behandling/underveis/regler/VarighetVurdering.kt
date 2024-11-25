package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetVurdering.Avslagsårsak

sealed interface VarighetVurdering {
    enum class Avslagsårsak {
        STANDARDKVOTE_BRUKT_OPP,
        STUDENTKVOTE_BRUKT_OPP
    }
}

data object Oppfylt: VarighetVurdering

data class Avslag(
    val avslagsårsaker: List<Avslagsårsak>
) : VarighetVurdering {
    constructor(avslagsårsak: Avslagsårsak): this(listOf(avslagsårsak))
}

