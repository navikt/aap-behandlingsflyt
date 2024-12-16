package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetVurdering.Avslagsårsak

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
    val avslagsårsaker: Set<Avslagsårsak>,
) : VarighetVurdering