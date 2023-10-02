package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovLøsning

class LøsAvklaringsbehovBehandlingHendelse(private val løsning: AvklaringsbehovLøsning, private val versjon: Long) :
    BehandlingHendelse {

    fun behov(): AvklaringsbehovLøsning {
        return løsning
    }

    fun versjon(): Long {
        return versjon
    }
}
