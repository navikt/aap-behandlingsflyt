package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class Avklaringsbehov(
    val id: Long,
    val definisjon: Definisjon,
    val historikk: MutableList<Endring> = mutableListOf(),
    val funnetISteg: StegType,
    private var kreverToTrinn: Boolean?
) {
    init {
        if (historikk.isEmpty()) {
            historikk += Endring(status = Status.OPPRETTET, begrunnelse = "", endretAv = "system")
        }
    }

    fun erTotrinn(): Boolean {
        if (definisjon.kreverToTrinn) {
            return true
        }
        return kreverToTrinn == true
    }

    fun erTotrinnsVurdert(): Boolean {
        return Status.TOTRINNS_VURDERT == historikk.last().status
    }

    fun vurderTotrinn(begrunnelse: String, godkjent: Boolean, vurdertAv: String) {
        require(definisjon.kreverToTrinn)
        val status = if (godkjent) {
            Status.TOTRINNS_VURDERT
        } else {
            Status.SENDT_TILBAKE_FRA_BESLUTTER
        }
        historikk += Endring(status = status, begrunnelse = begrunnelse, endretAv = vurdertAv)
    }

    fun reåpne() {
        historikk += Endring(status = Status.OPPRETTET, begrunnelse = "", endretAv = "system")
    }

    fun erÅpent(): Boolean {
        return status() in setOf(Status.OPPRETTET, Status.SENDT_TILBAKE_FRA_BESLUTTER)
    }

    fun skalStoppeHer(stegType: StegType): Boolean {
        return definisjon.skalLøsesISteg(stegType, funnetISteg) && erÅpent()
    }

    fun løs(begrunnelse: String, endretAv: String) {
        løs(begrunnelse, endretAv, definisjon.kreverToTrinn)
    }

    fun løs(begrunnelse: String, endretAv: String, kreverToTrinn: Boolean) {
        if (this.kreverToTrinn != true) {
            this.kreverToTrinn = kreverToTrinn
        }
        historikk.add(Endring(status = Status.AVSLUTTET, begrunnelse = begrunnelse, endretAv = endretAv))
    }

    fun avbryt() {
        historikk += Endring(status = Status.AVBRUTT, begrunnelse = "", endretAv = "system")
    }

    fun erIkkeAvbrutt(): Boolean {
        return Status.AVBRUTT != status()
    }

    fun erAvsluttet(): Boolean {
        return status() == Status.AVSLUTTET
    }

    fun status(): Status {
        return historikk.last().status
    }

    fun begrunnelse(): String = historikk.last().begrunnelse
    fun endretAv(): String = historikk.last().endretAv

    fun skalLøsesISteg(type: StegType): Boolean {
        return definisjon.skalLøsesISteg(type, funnetISteg)
    }

    fun erForeslåttVedtak(): Boolean {
        return definisjon == Definisjon.FORESLÅ_VEDTAK
    }

    fun harVærtSendtTilbakeFraBeslutterTidligere(): Boolean {
        return historikk.any { it.status == Status.SENDT_TILBAKE_FRA_BESLUTTER }
    }

    fun løsesISteg(): StegType {
        if (definisjon.løsesISteg == StegType.UDEFINERT) {
            return funnetISteg
        }
        return definisjon.løsesISteg
    }

    override fun toString(): String {
        return "Avklaringsbehov(definisjon=$definisjon, status=${status()})"
    }

}