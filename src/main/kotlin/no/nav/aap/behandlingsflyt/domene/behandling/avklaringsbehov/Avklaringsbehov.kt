package no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.flyt.steg.StegType

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

    fun vurderTotrinn(begrunnelse: String, godkjent: Boolean) {
        require(definisjon.kreverToTrinn)
        val status = if (godkjent) {
            Status.TOTRINNS_VURDERT
        } else {
            Status.SENDT_TILBAKE_FRA_BESLUTTER
        }
        historikk += Endring(status = status, begrunnelse = begrunnelse, endretAv = "system")
    }

    fun reåpne() {
        historikk += Endring(status = Status.OPPRETTET, begrunnelse = "", endretAv = "system")
    }

    fun erÅpent(): Boolean {
        return historikk.last().status in setOf(Status.OPPRETTET, Status.SENDT_TILBAKE_FRA_BESLUTTER)
    }

    fun skalStoppeHer(stegType: StegType): Boolean {
        return definisjon.skalLøsesISteg(stegType, funnetISteg) && erÅpent()
    }

    fun løs(begrunnelse: String, endretAv: String) {
        historikk.add(Endring(status = Status.AVSLUTTET, begrunnelse = begrunnelse, endretAv = endretAv))
    }

    fun erTotrinn(): Boolean {
        return definisjon.kreverToTrinn
    }

    fun erTotrinnsVurdert(): Boolean {
        return Status.TOTRINNS_VURDERT == historikk.last().status
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

    fun avbryt() {
        historikk += Endring(status = Status.AVBRUTT, begrunnelse = "", endretAv = "system")
    }

}
