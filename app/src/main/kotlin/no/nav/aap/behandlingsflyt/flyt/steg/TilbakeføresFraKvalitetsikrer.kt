package no.nav.aap.behandlingsflyt.flyt.steg

object TilbakeføresFraKvalitetsikrer : StegResultat {
    override fun transisjon(): Transisjon {
        return TilbakeførtFraKvalitetssikrer
    }
}