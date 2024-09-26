package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon

class StegResultat(
    val avklaringsbehov: List<Definisjon> = listOf(),
    val tilbakeførtFraBeslutter: Boolean = false,
    val tilbakeførtFraKvalitetssikrer: Boolean = false
) {

    fun transisjon(): Transisjon {
        if (tilbakeførtFraBeslutter) {
            return TilbakeførtFraBeslutter
        }
        if (tilbakeførtFraKvalitetssikrer) {
            return TilbakeførtFraKvalitetssikrer
        }
        if (avklaringsbehov.isNotEmpty()) {
            return FunnetAvklaringsbehov(avklaringsbehov)
        }
        return Fortsett
    }
}
