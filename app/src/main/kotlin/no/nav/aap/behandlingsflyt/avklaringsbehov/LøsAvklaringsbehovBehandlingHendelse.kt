package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovLøsning

class LøsAvklaringsbehovBehandlingHendelse(
    private val løsning: AvklaringsbehovLøsning,
    val ingenEndringIGruppe: Boolean = false,
    val behandlingVersjon: Long
) {

    fun behov(): AvklaringsbehovLøsning {
        return løsning
    }
}
