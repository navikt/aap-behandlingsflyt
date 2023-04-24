package no.nav.aap.flyt.kontroll

import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.StegType

interface Transisjon {
    fun funnetAvklaringsbehov(): List<Definisjon> = listOf()
    fun tilSteg(): StegType = StegType.UDEFINERT

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

class TilbakeførtTilAvklaringsbehov(var avklaringsbehov: List<Definisjon>, var tilSteg: StegType) : Transisjon {
    override fun funnetAvklaringsbehov(): List<Definisjon> {
        return avklaringsbehov
    }

    override fun tilSteg(): StegType {
        return tilSteg
    }

    override fun erTilbakeføring(): Boolean {
        return true
    }
}

class Tilbakeført(var tilSteg: StegType) : Transisjon {
    override fun tilSteg(): StegType {
        return tilSteg
    }

    override fun erTilbakeføring(): Boolean {
        return true
    }
}
