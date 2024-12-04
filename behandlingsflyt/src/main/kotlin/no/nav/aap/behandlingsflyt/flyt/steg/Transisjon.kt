package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon

sealed interface Transisjon {
    fun erTilbakeføring(): Boolean = false
    fun kanFortsette(): Boolean = true
}

object Fortsett : Transisjon
object Stopp : Transisjon {
    override fun kanFortsette(): Boolean {
        return false
    }
}

class FunnetAvklaringsbehov(private val avklaringsbehov: List<Definisjon>) : Transisjon {
    fun avklaringsbehov(): List<Definisjon> {
        return avklaringsbehov
    }
}

class FunnetVentebehov(private val ventebehov: List<Ventebehov>) : Transisjon {
    fun ventebehov(): List<Ventebehov> {
        return ventebehov
    }
}

object TilbakeførtFraBeslutter : Transisjon {
    override fun erTilbakeføring(): Boolean {
        return true
    }
}

object TilbakeførtFraKvalitetssikrer : Transisjon {
    override fun erTilbakeføring(): Boolean {
        return true
    }
}
