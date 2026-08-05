package no.nav.aap.behandlingsflyt.flyt.steg

data object Fullført : StegResultat {
    override fun transisjon(): Transisjon {
        return Fortsett
    }
}