package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.ÅrsakTilSettPåVent
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
        return Status.TOTRINNS_VURDERT == historikk.maxOf { it }.status
    }

    fun erKvalitetssikretTidligere(): Boolean {
        return Status.KVALITETSSIKRET == historikk.filter {
            it.status in setOf(
                Status.OPPRETTET,
                Status.KVALITETSSIKRET,
                Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER
            )
        }.maxOf { it }.status
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

    fun vurderKvalitet(
        begrunnelse: String,
        godkjent: Boolean,
        vurdertAv: String,
        årsakTilRetur: List<ÅrsakTilRetur>,
    ) {
        require(definisjon.kreverToTrinn)
        val status = if (godkjent) {
            Status.KVALITETSSIKRET
        } else {
            Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER
        }
        historikk += Endring(
            status = status,
            begrunnelse = begrunnelse,
            endretAv = vurdertAv,
            årsakTilRetur = årsakTilRetur,
        )
    }

    fun reåpne(frist: LocalDate? = null, begrunnelse: String = "", grunn: ÅrsakTilSettPåVent? = null) {
        historikk += Endring(
            status = Status.OPPRETTET,
            begrunnelse = begrunnelse,
            grunn = grunn,
            frist = frist,
            endretAv = SYSTEMBRUKER.ident
        )
    }

    fun erÅpent(): Boolean {
        return status() in setOf(
            Status.OPPRETTET,
            Status.SENDT_TILBAKE_FRA_BESLUTTER,
            Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER
        )
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
        return historikk.maxOf { it }.status
    }

    fun begrunnelse(): String = historikk.maxOf { it }.begrunnelse
    fun grunn(): ÅrsakTilSettPåVent = requireNotNull(historikk.maxOf { it }.grunn)
    fun endretAv(): String = historikk.maxOf { it }.endretAv
    fun årsakTilRetur(): List<ÅrsakTilRetur> = historikk.maxOf { it }.årsakTilRetur

    fun skalLøsesISteg(type: StegType): Boolean {
        return definisjon.skalLøsesISteg(type, funnetISteg)
    }

    fun erForeslåttVedtak(): Boolean {
        return definisjon == Definisjon.FORESLÅ_VEDTAK
    }

    fun harVærtSendtTilbakeFraBeslutterTidligere(): Boolean {
        return historikk.any { it.status == Status.SENDT_TILBAKE_FRA_BESLUTTER }
    }

    fun harVærtSendtTilbakeFraKvalitetssikrerTidligere(): Boolean {
        return historikk.any { it.status == Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER }
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
        return requireNotNull(historikk.last { it.status == Status.OPPRETTET }.frist)
    }

    fun fristUtløpt(): Boolean {
        return frist().isBefore(LocalDate.now()) || frist().isEqual(LocalDate.now())
    }

    fun kreverKvalitetssikring(): Boolean {
        return definisjon.kvalitetssikres
    }

    override fun toString(): String {
        return "Avklaringsbehov(definisjon=$definisjon, status=${status()})"
    }


}