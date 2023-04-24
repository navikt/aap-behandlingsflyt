package no.nav.aap.steg

import no.nav.aap.flyt.StegType

interface BehandlingSteg {

    fun utfør(input: StegInput): StegResultat

    fun type(): StegType
}
