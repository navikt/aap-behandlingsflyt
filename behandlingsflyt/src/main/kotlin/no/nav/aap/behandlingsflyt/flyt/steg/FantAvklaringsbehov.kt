package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon

@Deprecated("Styr avklaringsbehov med AvklaringsbehovService")
data class FantAvklaringsbehov(private val avklaringsbehov: Definisjon) : StegResultat {
    init {
        require(!avklaringsbehov.erVentebehov()) { "Inneholder ventebehov, disse bruk Stegresultat FantVentebehov:: $avklaringsbehov" }
    }

    override fun transisjon(): Transisjon {
        return FunnetAvklaringsbehov(avklaringsbehov)
    }
}