package no.nav.aap.behandlingsflyt.flyt.steg

data object TilbakeføresFraKvalitetsikrer : StegResultat {
    override fun transisjon(): Transisjon {
        return TilbakeførtFraKvalitetssikrer
    }
}