package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.VarighetVurdering.Avslagsårsak

sealed interface VarighetVurdering {
    fun og(other: VarighetVurdering): VarighetVurdering

    enum class Avslagsårsak {
        STANDARDKVOTE_BRUKT_OPP,
        STUDENTKVOTE_BRUKT_OPP,
        ETABLERINGSFASEKVOTE_BRUKT_OPP,
        UTVIKLINGSFASEKVOTE_BRUKT_OPP,
    }
}

data object Oppfylt: VarighetVurdering {
    override fun og(other: VarighetVurdering): VarighetVurdering {
        return other
    }
}

data class Avslag(
    val avslagsårsaker: Set<Avslagsårsak>
) : VarighetVurdering {
    constructor(avslagsårsak: Avslagsårsak): this(setOf(avslagsårsak))

    override fun og(other: VarighetVurdering): VarighetVurdering {
        return when (other) {
            is Avslag -> Avslag(this.avslagsårsaker + other.avslagsårsaker)
            Oppfylt -> this
        }
    }
}