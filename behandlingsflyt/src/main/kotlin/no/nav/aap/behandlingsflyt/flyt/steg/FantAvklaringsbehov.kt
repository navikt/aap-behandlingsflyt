package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon

@Deprecated("Styr avklaringsbehov med AvklaringsbehovService")
class FantAvklaringsbehov(private val avklaringsbehov: List<Definisjon>) : StegResultat {
    @Deprecated("Styr avklaringsbehov med AvklaringsbehovService")
    constructor(definisjon: Definisjon) : this(listOf(definisjon))

    init {
        require(avklaringsbehov.isNotEmpty())
        require(avklaringsbehov.none { it.erVentebehov() }) { "Inneholder ventebehov, disse bruk Stegresultat FantVentebehov:: $avklaringsbehov" }
    }

    override fun transisjon(): Transisjon {
        return FunnetAvklaringsbehov(avklaringsbehov)
    }

    override fun toString(): String {
        return "FantAvklaringsbehov(avklaringsbehov=$avklaringsbehov)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FantAvklaringsbehov

        return avklaringsbehov == other.avklaringsbehov
    }

    override fun hashCode(): Int {
        return avklaringsbehov.hashCode()
    }
}