package no.nav.aap.steg

import no.nav.aap.flyt.StegType

class GeneriskPlaceholderSteg(private val stegType: StegType) : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        return StegResultat() // DO NOTHING
    }

    override fun type(): StegType {
        return stegType
    }
}
