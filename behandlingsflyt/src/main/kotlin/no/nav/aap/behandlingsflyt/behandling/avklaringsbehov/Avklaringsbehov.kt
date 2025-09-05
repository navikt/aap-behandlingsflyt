package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.verdityper.Bruker
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
            historikk += Endring(
                status = Status.OPPRETTET, begrunnelse = "", endretAv = SYSTEMBRUKER.ident
            )
        }
    }

    fun erTotrinn(): Boolean {
        if (definisjon.kreverToTrinn) {
            return true
        }
        return kreverToTrinn == true
    }

    fun brukere(): List<String> {
        return historikk.filter { it.status == Status.AVSLUTTET }.map { it.endretAv }
    }

    fun erTotrinnsVurdert(): Boolean {
        return Status.TOTRINNS_VURDERT == historikk.maxOf { it }.status
    }

    fun erKvalitetssikretTidligere(): Boolean {
        return Status.KVALITETSSIKRET == historikk.filter {
            it.status in setOf(
                Status.KVALITETSSIKRET, Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER
            )
        }.maxOfOrNull { it }?.status
    }

    internal fun vurderTotrinn(
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

    internal fun vurderKvalitet(
        begrunnelse: String,
        godkjent: Boolean,
        vurdertAv: String,
        årsakTilRetur: List<ÅrsakTilRetur>,
    ) {
        require(definisjon.kvalitetssikres)
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

    internal fun reåpne(
        frist: LocalDate? = null,
        begrunnelse: String = "",
        venteårsak: ÅrsakTilSettPåVent? = null,
        bruker: Bruker = SYSTEMBRUKER
    ) {
        require(historikk.last().status.erAvsluttet())
        if (definisjon.erVentebehov()) {
            requireNotNull(frist)
            requireNotNull(venteårsak)
        }
        historikk += Endring(
            status = Status.OPPRETTET,
            begrunnelse = begrunnelse,
            grunn = venteårsak,
            frist = frist,
            endretAv = bruker.ident
        )
    }

    fun erÅpent(): Boolean {
        return status().erÅpent()
    }

    fun skalStoppeHer(stegType: StegType): Boolean {
        return definisjon.skalLøsesISteg(stegType, funnetISteg) && erÅpent()
    }

    internal fun løs(begrunnelse: String, endretAv: String) {
        løs(begrunnelse, endretAv, definisjon.kreverToTrinn)
    }

    internal fun løs(begrunnelse: String, endretAv: String, kreverToTrinn: Boolean) {
        if (this.kreverToTrinn != true) {
            this.kreverToTrinn = kreverToTrinn
        }
        historikk.add(
            Endring(
                status = Status.AVSLUTTET, begrunnelse = begrunnelse, endretAv = endretAv
            )
        )
    }

    internal fun avbryt() {
        historikk += Endring(
            status = Status.AVBRUTT, begrunnelse = "", endretAv = SYSTEMBRUKER.ident
        )
    }

    internal fun avslutt() {
        check(historikk.any { it.status == Status.AVSLUTTET }) {
            "Et steg burde vel ha vært løst minst en gang for å kunne regnes som avsluttet?"
        }

        historikk += Endring(
            status = Status.AVSLUTTET, begrunnelse = "", endretAv = SYSTEMBRUKER.ident
        )
    }

    fun erIkkeAvbrutt(): Boolean {
        return Status.AVBRUTT != status()
    }

    fun erAvsluttet(): Boolean {
        return status().erAvsluttet()
    }

    fun harAvsluttetStatusIHistorikken(): Boolean {
        return historikk.any { it.status == Status.AVSLUTTET }
    }

    fun status(): Status {
        return historikk.maxOf { it }.status
    }

    fun begrunnelse(): String = historikk.maxOf { it }.begrunnelse
    fun venteårsak(): ÅrsakTilSettPåVent? = historikk.filter { it.status == Status.OPPRETTET }.maxOf { it }.grunn
    fun endretAv(): String = historikk.maxOf { it }.endretAv
    fun årsakTilRetur(): List<ÅrsakTilRetur> = historikk.maxOf { it }.årsakTilRetur

    fun skalLøsesISteg(type: StegType): Boolean {
        return definisjon.skalLøsesISteg(type, funnetISteg)
    }

    fun skalLøsesIStegGruppe(gruppe: StegGruppe): Boolean {
        val steg = StegType.entries.filter { it.gruppe == gruppe }
        return steg.any { skalLøsesISteg(it) }
    }

    fun erForeslåttVedtak(): Boolean {
        return definisjon == Definisjon.FORESLÅ_VEDTAK
    }
    
    fun erForeslåttUttak(): Boolean {
        return definisjon == Definisjon.FORESLÅ_UTTAK
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
        return requireNotNull(historikk.last { it.status == Status.OPPRETTET && it.frist != null }.frist)
        { "Prøvde å finne frist, men historikk er tom. Definisjon $definisjon. Funnet i steg $funnetISteg. ID: $id. Historikk: $historikk." }
    }

    fun fristUtløpt(): Boolean {
        return frist().isBefore(LocalDate.now()) || frist().isEqual(LocalDate.now())
    }

    fun kreverKvalitetssikring(): Boolean {
        return definisjon.kvalitetssikres
    }

    fun erAutomatisk(): Boolean {
        return definisjon.erAutomatisk()
    }

    fun erBrevVentebehov(): Boolean {
        return definisjon.erBrevVentebehov()
    }

    override fun toString(): String {
        return "Avklaringsbehov(definisjon=$definisjon, status=${status()}, løsesISteg=${løsesISteg()})"
    }
}