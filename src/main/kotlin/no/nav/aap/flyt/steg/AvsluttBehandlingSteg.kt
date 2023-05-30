package no.nav.aap.flyt.steg

import no.nav.aap.flyt.StegType

class AvsluttBehandlingSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        return StegResultat() // DO NOTHING
    }

    override fun type(): StegType {
        return StegType.AVSLUTT_BEHANDLING
    }
}
