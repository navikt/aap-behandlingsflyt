package no.nav.aap.mottak

import no.nav.aap.avklaringsbehov.AvklaringsbehovLøsning

class LøsAvklaringsbehovBehandlingHendelse(private val løsning: AvklaringsbehovLøsning, private val versjon: Long) : BehandlingHendelse {

    fun behov(): AvklaringsbehovLøsning {
        return løsning
    }

    fun versjon(): Long {
        return versjon
    }
}
