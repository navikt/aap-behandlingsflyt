package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetVurdering.Avslagsårsak

sealed interface VarighetVurdering {
    enum class Avslagsårsak {
        STANDARDKVOTE_BRUKT_OPP,
        STUDENTKVOTE_BRUKT_OPP,
        ETABLERINGSFASEKVOTE_BRUKT_OPP,
        UTVIKLINGSFASEKVOTE_BRUKT_OPP,
    }
}

data object Oppfylt: VarighetVurdering

data class Avslag(
    val avslagsårsaker: Set<Avslagsårsak>
) : VarighetVurdering