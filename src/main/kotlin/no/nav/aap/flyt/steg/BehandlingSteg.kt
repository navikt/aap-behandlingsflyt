package no.nav.aap.flyt.steg

import no.nav.aap.flyt.StegType

interface BehandlingSteg {

    fun utfør(input: StegInput): StegResultat

    fun type(): StegType

    fun vedTilbakeføring(input: StegInput) {

    }
}
