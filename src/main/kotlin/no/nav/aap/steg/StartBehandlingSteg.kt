package no.nav.aap.steg

import no.nav.aap.flyt.StegType

class StartBehandlingSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        return StegResultat() // DO NOTHING
    }

    override fun type(): StegType {
        return StegType.START_BEHANDLING
    }
}
