package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon

interface Transisjon {
    fun funnetAvklaringsbehov(): List<Definisjon> = listOf()

    fun erTilbakeføring(): Boolean = false
    fun kanFortsette(): Boolean = true
}

object Fortsett : Transisjon
object Stopp : Transisjon {
    override fun kanFortsette(): Boolean {
        return false
    }
}

class FunnetAvklaringsbehov(var avklaringsbehov: List<Definisjon>) : Transisjon {
    override fun funnetAvklaringsbehov(): List<Definisjon> {
        return avklaringsbehov
    }
}

object TilbakeførtFraBeslutter : Transisjon {
    override fun erTilbakeføring(): Boolean {
        return true
    }
}
