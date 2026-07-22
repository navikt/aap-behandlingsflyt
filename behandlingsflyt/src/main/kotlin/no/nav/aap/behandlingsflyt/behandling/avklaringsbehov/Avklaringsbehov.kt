package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.LocalDate
import java.time.LocalDateTime

class Avklaringsbehov(
    val id: Long,
    val definisjon: Definisjon,
    historikk: List<Endring> = mutableListOf(),
    val funnetISteg: StegType,
    private var kreverToTrinn: Boolean?
) {

    private var historikkInternal: MutableList<Endring> = historikk.toMutableList()

    init {
        if (historikkInternal.isEmpty()) {
            historikkInternal += Endring(
                status = Status.OPPRETTET, begrunnelse = "", endretAv = SYSTEMBRUKER
            )
        }
    }

    val aktivHistorikk: List<Endring>
        get() = historikkInternal.takeLastWhile {
            it.status != Status.AVBRUTT
        }

    val perioderVedtaketBehøverVurdering: List<Periode>?
        get() = aktivHistorikk
            .lastOrNull { it.status.erÅpent() }
            ?.perioderVedtaketBehøverVurdering
            ?.sorted()

    fun historikk(): List<Endring> = historikkInternal

    fun erTotrinn(): Boolean {
        if (definisjon.kreverToTrinn) {
            return true
        }
        return kreverToTrinn == true
    }

    fun brukere(): List<Bruker> {
        return historikkInternal.filter { it.status == Status.AVSLUTTET }.map { it.endretAv }
    }

    fun erTotrinnsVurdert(): Boolean {
        return Status.TOTRINNS_VURDERT == aktivHistorikk.maxOfOrNull { it }?.status
    }

    fun erKvalitetssikret(): Boolean {
        return Status.KVALITETSSIKRET == aktivHistorikk.maxOfOrNull { it }?.status
    }

    fun harBlittKvalitetssikretTidligere(): Boolean {
        return Status.KVALITETSSIKRET == aktivHistorikk.filter {
            it.status in setOf(
                Status.KVALITETSSIKRET, Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER
            )
        }.maxOfOrNull { it }?.status
    }

    internal fun vurderTotrinn(
        begrunnelse: String,
        godkjent: Boolean,
        vurdertAv: Bruker,
        årsakTilRetur: List<ÅrsakTilRetur>,
    ) {
        require(definisjon.kreverToTrinn)
        val status = if (godkjent) {
            Status.TOTRINNS_VURDERT
        } else {
            Status.SENDT_TILBAKE_FRA_BESLUTTER
        }
        historikkInternal += Endring(
            status = status,
            begrunnelse = begrunnelse,
            endretAv = vurdertAv,
            årsakTilRetur = årsakTilRetur,
        )
    }

    internal fun vurderKvalitet(
        begrunnelse: String,
        godkjent: Boolean,
        vurdertAv: Bruker,
        årsakTilRetur: List<ÅrsakTilRetur>,
    ) {
        require(definisjon.kvalitetssikres)
        val status = if (godkjent) {
            Status.KVALITETSSIKRET
        } else {
            Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER
        }
        historikkInternal += Endring(
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
        bruker: Bruker = SYSTEMBRUKER,
        perioderVedtaketBehøverVurdering: Set<Periode>?,
        perioderSomIkkeErTilstrekkeligVurdert: Set<Periode>?,
    ) {
        require(historikkInternal.last().status.erAvsluttet()) { "Krever at status er avsluttet for å reåpne. Var: ${historikkInternal.last().status}." }
        if (definisjon.erVentebehov()) {
            requireNotNull(frist)
            requireNotNull(venteårsak)
        }
        historikkInternal += Endring(
            status = Status.OPPRETTET,
            begrunnelse = begrunnelse,
            grunn = venteårsak,
            frist = frist,
            endretAv = bruker,
            perioderVedtaketBehøverVurdering = perioderVedtaketBehøverVurdering,
            perioderSomIkkeErTilstrekkeligVurdert = perioderSomIkkeErTilstrekkeligVurdert
        )
    }

    internal fun oppdaterPerioder(
        perioderSomIkkeErTilstrekkeligVurdert: Set<Periode>?,
        perioderVedtaketBehøverVurdering: Set<Periode>?
    ): Boolean {
        val siste = historikkInternal.last()
        require(siste.status.erÅpent()) {
            "Prøvde å oppdatere perioder på et lukket avklaringsbehov"
        }
        if (perioderSomIkkeErTilstrekkeligVurdert != siste.perioderSomIkkeErTilstrekkeligVurdert || perioderVedtaketBehøverVurdering != siste.perioderVedtaketBehøverVurdering) {
            historikkInternal += siste.copy(
                perioderSomIkkeErTilstrekkeligVurdert = perioderSomIkkeErTilstrekkeligVurdert,
                perioderVedtaketBehøverVurdering = perioderVedtaketBehøverVurdering,
                tidsstempel = LocalDateTime.now()
            )
            return true
        }
        return false
    }

    fun erÅpent(): Boolean {
        return status().erÅpent()
    }

    fun skalStoppeHer(stegType: StegType): Boolean {
        return definisjon.skalLøsesISteg(stegType, funnetISteg) && erÅpent()
    }

    internal fun løs(begrunnelse: String, endretAv: Bruker) {
        løs(begrunnelse, endretAv, definisjon.kreverToTrinn)
    }

    internal fun løs(begrunnelse: String, endretAv: Bruker, kreverToTrinn: Boolean) {
        if (this.kreverToTrinn != true) {
            this.kreverToTrinn = kreverToTrinn
        }
        historikkInternal.add(
            Endring(
                status = Status.AVSLUTTET, begrunnelse = begrunnelse, endretAv = endretAv
            )
        )
    }

    internal fun avbryt() {
        historikkInternal += Endring(
            status = Status.AVBRUTT, begrunnelse = "", endretAv = SYSTEMBRUKER
        )
    }

    internal fun avslutt(begrunnelse: String) {
        check(historikkInternal.any { it.status == Status.AVSLUTTET }) {
            "Et steg burde vel ha vært løst minst en gang for å kunne regnes som avsluttet?"
        }

        historikkInternal += Endring(
            status = Status.AVSLUTTET, begrunnelse = begrunnelse, endretAv = SYSTEMBRUKER
        )
    }

    fun erIkkeAvbrutt(): Boolean {
        return Status.AVBRUTT != status()
    }

    fun erAvsluttet(): Boolean {
        return status().erAvsluttet()
    }

    fun harAvsluttetStatusIHistorikken(): Boolean {
        return historikkInternal.any { it.status == Status.AVSLUTTET }
    }

    fun sistAvsluttet(): LocalDateTime {
        return historikkInternal.filter { it.status == Status.AVSLUTTET }.maxOf { it.tidsstempel }
    }

    fun sistAvsluttetOrNull(): LocalDateTime? {
        return historikkInternal.filter { it.status == Status.AVSLUTTET }.maxOfOrNull { it.tidsstempel }
    }

    fun status(): Status {
        return historikkInternal.maxOf { it }.status
    }

    fun begrunnelse(): String = historikkInternal.maxOf { it }.begrunnelse
    fun venteårsak(): ÅrsakTilSettPåVent? = historikkInternal.filter { it.status == Status.OPPRETTET }.maxOf { it }.grunn
    fun endretAv(): Bruker = historikkInternal.maxOf { it }.endretAv
    fun årsakTilRetur(): List<ÅrsakTilRetur> = historikkInternal.maxOf { it }.årsakTilRetur

    fun skalLøsesISteg(type: StegType): Boolean {
        return definisjon.skalLøsesISteg(type, funnetISteg)
    }

    fun erForeslåttVedtak(): Boolean {
        return definisjon == Definisjon.FORESLÅ_VEDTAK
    }

    fun erForeslåttVedtakVedtakslengde(): Boolean {
        return definisjon == Definisjon.FORESLÅ_VEDTAK_VEDTAKSLENGDE
    }

    fun erLovvalgOgMedlemskap(): Boolean {
        return definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP
    }

    fun harVærtSendtTilbakeFraBeslutterTidligere(): Boolean {
        return aktivHistorikk.any { it.status == Status.SENDT_TILBAKE_FRA_BESLUTTER }
    }

    fun harVærtSendtTilbakeFraKvalitetssikrerTidligere(): Boolean {
        return aktivHistorikk.any { it.status == Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER }
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
        return requireNotNull(historikkInternal.last { it.status == Status.OPPRETTET && it.frist != null }.frist)
        { "Prøvde å finne frist, men historikk er tom. Definisjon $definisjon. Funnet i steg $funnetISteg. ID: $id. Historikk: $historikkInternal." }
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

    fun sistEndret(): LocalDateTime {
        return historikkInternal.last().tidsstempel
    }

    fun perioderVedtaketBehøverVurdering(): Set<Periode>? {
        return perioderVedtaketBehøverVurdering?.toSet()
    }

    fun perioderSomIkkeErTilstrekkeligVurdert(): Set<Periode>? {
        return aktivHistorikk.filter { it.status.erÅpent() }.maxOfOrNull { it }?.perioderSomIkkeErTilstrekkeligVurdert
    }


    override fun toString(): String {
        return "Avklaringsbehov(definisjon=$definisjon, status=${status()}, løsesISteg=${løsesISteg()})"
    }
}