package no.nav.aap.domene.behandling.avklaringsbehov

import no.nav.aap.flyt.StegStatus
import no.nav.aap.flyt.StegType

class Avklaringsbehov(
    val definisjon: Definisjon,
    val historikk: MutableList<Endring> = mutableListOf(),
    val funnetISteg: StegType
) {
    init {
        if (historikk.isEmpty()) {
            historikk += Endring(status = Status.OPPRETTET, begrunnelse = "", endretAv = "system")
        }
    }

    fun reåpne() {
        historikk += Endring(status = Status.OPPRETTET, begrunnelse = "", endretAv = "system")
    }

    fun erÅpent(): Boolean {
        return Status.OPPRETTET == historikk.last().status
    }

    fun skalStoppeHer(stegType: StegType, stegStatus: StegStatus): Boolean {
        return definisjon.skalLøsesISteg(stegType, funnetISteg) && definisjon.påStegStatus(stegStatus) && erÅpent()
    }

    fun løs(begrunnelse: String, endretAv: String) {
        historikk.add(Endring(status = Status.AVSLUTTET, begrunnelse = begrunnelse, endretAv = endretAv))
    }

    fun erTotrinn(): Boolean {
        return definisjon.kreverToTrinn
    }

    fun erIkkeAvbrutt(): Boolean {
        return Status.AVBRUTT != historikk.last().status
    }

    fun status(): Status {
        return historikk.last().status
    }

    override fun toString(): String {
        return "Avklaringsbehov(definisjon=$definisjon, status=${status()})"
    }

    fun skalLøsesISteg(type: StegType): Boolean {
        return definisjon.skalLøsesISteg(type, funnetISteg)
    }

    fun løsesISteg(): StegType {
        if (definisjon.løsesISteg == StegType.UDEFINERT) {
            return funnetISteg
        }
        return definisjon.løsesISteg

    }

}
