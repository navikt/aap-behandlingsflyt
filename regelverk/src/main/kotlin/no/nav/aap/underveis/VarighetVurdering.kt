package no.nav.aap.underveis

import no.nav.aap.kvote.Kvote

sealed interface VarighetVurdering {
    enum class Avslagsårsak {
        ORDINÆRKVOTE_BRUKT_OPP,
        STUDENTKVOTE_BRUKT_OPP,
        ETABLERINGSFASEKVOTE_BRUKT_OPP,
        UTVIKLINGSFASEKVOTE_BRUKT_OPP,
        SYKEPENGEERSTATNINGKVOTE_BRUKT_OPP,
    }
    val brukerAvKvoter: Set<Kvote>
}

data class Oppfylt(
    override val brukerAvKvoter: Set<Kvote>,
): VarighetVurdering

data class Avslag(
    override val brukerAvKvoter: Set<Kvote>,
    val avslagsårsaker: Set<VarighetVurdering.Avslagsårsak>,
) : VarighetVurdering