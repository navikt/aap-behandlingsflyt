package no.nav.aap.behandlingsflyt.flyt.steg

data class FantVentebehov(private val ventebehov: Ventebehov) : StegResultat {

    init {
        require(ventebehov.definisjon.erVentebehov()) { "Inneholder avklaringsbehov. Bruk Stegresultat FantAvklaringsbehov for disse :: $ventebehov" }
    }

    override fun transisjon(): Transisjon {
        return FunnetVentebehov(ventebehov)
    }
}