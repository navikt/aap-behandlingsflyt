package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon

sealed interface Transisjon {
    fun kanFortsette(): Boolean = true
}

object Fortsett : Transisjon
object Stopp : Transisjon {
    override fun kanFortsette(): Boolean {
        return false
    }
}

class FunnetAvklaringsbehov(private val avklaringsbehov: Definisjon) : Transisjon {
    fun avklaringsbehov(): Definisjon {
        return avklaringsbehov
    }
}

class FunnetVentebehov(private val ventebehov: Ventebehov) : Transisjon {
    fun ventebehov(): Ventebehov {
        return ventebehov
    }
}