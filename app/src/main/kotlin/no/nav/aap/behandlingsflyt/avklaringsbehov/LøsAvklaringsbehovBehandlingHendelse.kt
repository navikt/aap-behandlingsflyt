package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.auth.Bruker
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.AvklaringsbehovLøsning

class LøsAvklaringsbehovBehandlingHendelse(
    private val løsning: AvklaringsbehovLøsning,
    val ingenEndringIGruppe: Boolean = false,
    val behandlingVersjon: Long,
    val bruker: Bruker
) {

    fun behov(): AvklaringsbehovLøsning {
        return løsning
    }
}
