package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.verdityper.flyt.StegType
import java.time.LocalDate

class Avklaringsbehov(
    val id: Long,
    val definisjon: Definisjon,
    val historikk: MutableList<Endring> = mutableListOf(),
    val funnetISteg: StegType,
    private var kreverToTrinn: Boolean?
) {
    init {
        if (historikk.isEmpty()) {
            historikk += Endring(status = Status.OPPRETTET, begrunnelse = "", endretAv = SYSTEMBRUKER.ident)
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

    fun vurderTotrinn(
        begrunnelse: String,
        godkjent: Boolean,
        vurdertAv: String,
        årsakTilRetur: List<ÅrsakTilRetur>,
    ) {
        require(definisjon.kreverToTrinn)
        val status = if (godkjent) {
            Status.TOTRINNS_VURDERT
        } else {
            Status.SENDT_TILBAKE_FRA_BESLUTTER
        }
        historikk += Endring(
            status = status,
            begrunnelse = begrunnelse,
            endretAv = vurdertAv,
            årsakTilRetur = årsakTilRetur,
        )
    }

    fun reåpne() {
        historikk += Endring(status = Status.OPPRETTET, begrunnelse = "", endretAv = SYSTEMBRUKER.ident)
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
        historikk += Endring(status = Status.AVBRUTT, begrunnelse = "", endretAv = SYSTEMBRUKER.ident)
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
    fun årsakTilRetur(): List<ÅrsakTilRetur> = historikk.last().årsakTilRetur

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

    fun erVentepunkt(): Boolean {
        return definisjon.type == Definisjon.BehovType.VENTEPUNKT
    }

    fun frist(): LocalDate {
        return requireNotNull(historikk.last().frist)
    }

    fun fristUtløpt(): Boolean {
        return frist().isBefore(LocalDate.now()) || frist().isEqual(LocalDate.now())
    }

    override fun toString(): String {
        return "Avklaringsbehov(definisjon=$definisjon, status=${status()})"
    }

}