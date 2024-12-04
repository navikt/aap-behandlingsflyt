package no.nav.aap.behandlingsflyt.flyt.steg

class FantVentebehov(private val ventebehov: List<Ventebehov>) : StegResultat {
    constructor(ventebehov: Ventebehov) : this(listOf(ventebehov))

    init {
        require(ventebehov.isNotEmpty())
        require(ventebehov.all { it.definisjon.erVentebehov() }) { "Inneholder avklaringsbehov bruk Stegresultat FantAvklaringsbehov for disse :: $ventebehov" }
    }

    override fun transisjon(): Transisjon {
        return FunnetVentebehov(ventebehov)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FantVentebehov

        return ventebehov == other.ventebehov
    }

    override fun hashCode(): Int {
        return ventebehov.hashCode()
    }

    override fun toString(): String {
        return "FantVentebehov(ventebehov=$ventebehov)"
    }
}