package no.nav.aap.behandlingsflyt.flyt.steg

sealed interface StegResultat {

    fun transisjon(): Transisjon
}