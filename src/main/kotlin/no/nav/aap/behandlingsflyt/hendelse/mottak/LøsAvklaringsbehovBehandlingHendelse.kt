package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovLøsning

class LøsAvklaringsbehovBehandlingHendelse(private val løsning: AvklaringsbehovLøsning, val ingenEndringIGruppe: Boolean = false ) {

    fun behov(): AvklaringsbehovLøsning {
        return løsning
    }
}
