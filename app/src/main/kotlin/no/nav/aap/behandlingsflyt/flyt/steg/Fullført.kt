package no.nav.aap.behandlingsflyt.flyt.steg

object Fullført : StegResultat {
    override fun transisjon(): Transisjon {
        return Fortsett
    }

    override fun toString(): String {
        return "Fullført()"
    }
}