package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon

class StegResultat(
    val avklaringsbehov: List<Definisjon> = listOf(),
    val tilbakeførtFraBeslutter: Boolean = false
) {

    fun transisjon(): Transisjon {
        if (tilbakeførtFraBeslutter) {
            return TilbakeførtFraBeslutter
        }
        if (avklaringsbehov.isNotEmpty()) {
            return FunnetAvklaringsbehov(avklaringsbehov)
        }
        return Fortsett
    }
}
