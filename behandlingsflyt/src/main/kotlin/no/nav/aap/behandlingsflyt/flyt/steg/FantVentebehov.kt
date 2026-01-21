package no.nav.aap.behandlingsflyt.flyt.steg

data class FantVentebehov(private val ventebehov: Ventebehov) : StegResultat {

    init {
//        require(ventebehov.isNotEmpty())
//        require(ventebehov.all { it.definisjon.erVentebehov() }) { "Inneholder avklaringsbehov bruk Stegresultat FantAvklaringsbehov for disse :: $ventebehov" }
    }

    override fun transisjon(): Transisjon {
        return FunnetVentebehov(ventebehov)
    }
}