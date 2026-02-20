package no.nav.aap.behandlingsflyt.flyt.steg

data object TilbakeføresFraBeslutter : StegResultat {
    override fun transisjon(): Transisjon {
        return TilbakeførtFraBeslutter
    }
}