package no.nav.aap.behandlingsflyt.flyt.steg

object TilbakeføresFraBeslutter : StegResultat {
    override fun transisjon(): Transisjon {
        return TilbakeførtFraBeslutter
    }
}