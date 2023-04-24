package no.nav.aap.domene.behandling.avklaringsbehov

import no.nav.aap.flyt.StegStatus
import no.nav.aap.flyt.StegType

class Avklaringsbehov(var definisjon: Definisjon, var status: Status, var historikk: List<Endring> = listOf()) {

    // TODO: Trenger identifikator, knyttning til steg, type,

    fun reåpne() {
        historikk += Endring(status = Status.OPPRETTET, begrunnelse = "", endretAv = "system")
    }

    fun erÅpent(): Boolean {
        return true
    }

    fun skalStoppeHer(stegType: StegType, stegStatus: StegStatus): Boolean {
        return definisjon.løsesISteg(stegType)
    }
}
