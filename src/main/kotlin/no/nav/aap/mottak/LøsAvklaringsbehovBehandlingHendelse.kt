package no.nav.aap.mottak

import no.nav.aap.avklaringsbehov.AvklaringsbehovLøsning

class LøsAvklaringsbehovBehandlingHendelse(private val behandlingId: Long,
                                           private val løsning: AvklaringsbehovLøsning) : BehandlingHendelse {
    override fun behandlingId(): Long {
        return behandlingId
    }

    fun behov(): AvklaringsbehovLøsning {
        return løsning
    }
}
