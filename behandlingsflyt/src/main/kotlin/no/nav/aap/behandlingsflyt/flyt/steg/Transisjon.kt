package no.nav.aap.behandlingsflyt.flyt.steg

sealed interface Transisjon {
    fun kanFortsette(): Boolean = true
}

object Fortsett : Transisjon
object Stopp : Transisjon {
    override fun kanFortsette(): Boolean {
        return false
    }
}

class FunnetVentebehov(private val ventebehov: Ventebehov) : Transisjon {
    fun ventebehov(): Ventebehov {
        return ventebehov
    }
}
