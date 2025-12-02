package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.PeriodisertAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.tilgang.Rolle
import org.slf4j.LoggerFactory
import java.time.LocalDate

class Avklaringsbehovene(
    private val repository: AvklaringsbehovOperasjonerRepository,
    private val behandlingId: BehandlingId
) : AvklaringsbehoveneDecorator {
    private val log = LoggerFactory.getLogger(javaClass)
    private val avklaringsbehovene: List<Avklaringsbehov>
        get() = repository.hent(behandlingId)

    fun ingenEndring(avklaringsbehov: Avklaringsbehov, bruker: String) {
        løsAvklaringsbehov(
            avklaringsbehov.definisjon,
            "Ingen endring fra forrige vurdering",
            bruker
        )
    }

    fun løsAvklaringsbehov(
        definisjon: Definisjon,
        begrunnelse: String,
        endretAv: String,
        kreverToTrinn: Boolean? = null
    ) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        if (kreverToTrinn == null) {
            avklaringsbehov.løs(begrunnelse, endretAv = endretAv)
        } else {
            avklaringsbehov.løs(begrunnelse = begrunnelse, endretAv = endretAv, kreverToTrinn = kreverToTrinn)
            repository.kreverToTrinn(avklaringsbehov.id, kreverToTrinn)
        }
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
    }

    fun leggTilFrivilligHvisMangler(definisjon: Definisjon, bruker: Bruker) {
        if (definisjon.erFrivillig()) {
            if (hentBehovForDefinisjon(definisjon) == null) {
                // Legger til frivillig behov
                leggTil(definisjoner = listOf(definisjon), funnetISteg = definisjon.løsesISteg, bruker = bruker, perioderVedtaketBehøverVurdering = null, perioderSomIkkeErTilstrekkeligVurdert = null)
            }
        }
    }

    fun leggTilOverstyringHvisMangler(definisjon: Definisjon, bruker: Bruker) {
        if (definisjon.erOverstyring()) {
            if (hentBehovForDefinisjon(definisjon) == null) {
                leggTil(definisjoner = listOf(definisjon), funnetISteg = definisjon.løsesISteg, bruker = bruker, perioderVedtaketBehøverVurdering = null, perioderSomIkkeErTilstrekkeligVurdert = null)
            }
        }
    }

    /**
     * Legger til nye avklaringsbehov.
     *
     * NB! Dersom avklaringsbehovet finnes fra før og er åpent så ignorerer vi det nye behovet, mens dersom det er avsluttet eller avbrutt så reåpner vi det.
     */
    fun leggTil(
        definisjoner: List<Definisjon>,
        funnetISteg: StegType,
        perioderSomIkkeErTilstrekkeligVurdert: Set<Periode>?,
        perioderVedtaketBehøverVurdering: Set<Periode>?,
        frist: LocalDate? = null,
        begrunnelse: String = "",
        grunn: ÅrsakTilSettPåVent? = null,
        bruker: Bruker = SYSTEMBRUKER
    ) {
        log.info("Legger til avklaringsbehov :: {} - {}", definisjoner, funnetISteg)
        definisjoner.forEach { definisjon ->
            val avklaringsbehov = hentBehovForDefinisjon(definisjon)
            if (avklaringsbehov != null) {
                if (avklaringsbehov.erAvsluttet()) {
                    avklaringsbehov.reåpne(utledFrist(definisjon, frist), begrunnelse, grunn, perioderSomIkkeErTilstrekkeligVurdert, perioderVedtaketBehøverVurdering, bruker)
                    if (avklaringsbehov.erVentepunkt() || avklaringsbehov.erBrevVentebehov() || avklaringsbehov.erAutomatisk()) {
                        // TODO: Vurdere om funnet steg bør ligge på endringen...
                        repository.endreVentepunkt(avklaringsbehov.id, avklaringsbehov.historikk.last(), funnetISteg)
                    } else if (avklaringsbehov.definisjon == Definisjon.SKRIV_BREV) {
                        // Midlertidig fiks så lenge vi bruker Definisjon.SKRIV_BREV
                        repository.endreSkrivBrev(avklaringsbehov.id, avklaringsbehov.historikk.last(), funnetISteg)
                    } else {
                        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
                    }
                } else {
                    log.info("Forsøkte å legge til et avklaringsbehov som allerede eksisterte")
                }
            } else {
                repository.opprett(
                    behandlingId = behandlingId,
                    definisjon = definisjon,
                    funnetISteg = funnetISteg,
                    frist = utledFrist(definisjon, frist),
                    begrunnelse = begrunnelse,
                    grunn = grunn,
                    perioderSomIkkeErTilstrekkeligVurdert = perioderSomIkkeErTilstrekkeligVurdert,
                    perioderVedtaketBehøverVurdering = perioderVedtaketBehøverVurdering,
                    endretAv = bruker.ident
                )
            }
        }
    }

    private fun utledFrist(definisjon: Definisjon, frist: LocalDate?): LocalDate? {
        if (definisjon.erVentebehov()) {
            return definisjon.utledFrist(frist)
        }
        return null
    }

    fun vurderTotrinn(
        definisjon: Definisjon,
        godkjent: Boolean,
        begrunnelse: String,
        vurdertAv: String,
        årsakTilRetur: List<ÅrsakTilRetur> = emptyList(),
    ) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.vurderTotrinn(begrunnelse, godkjent, vurdertAv, årsakTilRetur)
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
    }

    fun vurderKvalitet(
        definisjon: Definisjon,
        godkjent: Boolean,
        begrunnelse: String,
        vurdertAv: String,
        årsakTilRetur: List<ÅrsakTilRetur> = emptyList(),
    ) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.vurderKvalitet(begrunnelse, godkjent, vurdertAv, årsakTilRetur)
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
    }

    @Deprecated("Bruk hjelpemetoder i AvklaringsbehovService for å styre avklaringsbehov")
    fun avbryt(definisjon: Definisjon) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.avbryt()
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
    }

    /* Kan fjerne internal-prefikset når deprecated `avbryt` er slettet. */
    internal fun internalAvbryt(definisjon: Definisjon) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.avbryt()
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
    }

    @Deprecated("Styr avklaringsbehov med AvklaringsbehovService")
    fun avbrytForSteg(steg: StegType) {
        for (avklaringsbehov in åpne()) {
            if (avklaringsbehov.skalLøsesISteg(steg)) {
                avklaringsbehov.avbryt()
                repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
            }
        }
    }

    @Deprecated("Styr avklaringsbehov med AvklaringsbehovService")
    fun avslutt(definisjon: Definisjon) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.avslutt()
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
    }

    /* Kan fjerne internal-prefixet når den deperecated avslutt-metoden er borte. */
    internal fun internalAvslutt(definisjon: Definisjon) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.avslutt()
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
    }

    @Deprecated("Styr avklaringsbehov med AvklaringsbehovService")
    fun avbrytÅpneAvklaringsbehov() {
        val avklaringsbehovSomSkalAvbrytes =
            alle().filter { it.erÅpent() && !(it.erVentepunkt() || it.erBrevVentebehov()) }
        avklaringsbehovSomSkalAvbrytes.map { it.definisjon }.distinct().forEach { avbryt(it) }
    }


    fun reåpne(definisjon: Definisjon) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        val frist = if (definisjon.erVentebehov()) {
            avklaringsbehov.frist()
        } else {
            null
        }
        avklaringsbehov.reåpne(
            frist = frist,
            venteårsak = avklaringsbehov.venteårsak(),
            perioderSomIkkeErTilstrekkeligVurdert = avklaringsbehov.perioderSomIkkeErTilstrekkeligVurdert(),
            perioderVedtaketBehøverVurdering = avklaringsbehov.perioderSomSkalLøses()
        )
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
    }

    fun oppdaterPerioder(definisjon: Definisjon, perioderSomIkkeErTilstrekkeligVurdert: Set<Periode>?, perioderVedtaketBehøverVurdering: Set<Periode>?) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.oppdaterPerioder(perioderSomIkkeErTilstrekkeligVurdert = perioderSomIkkeErTilstrekkeligVurdert, perioderVedtaketBehøverVurdering = perioderVedtaketBehøverVurdering)
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk.last())
    }


    override fun alle(): List<Avklaringsbehov> {
        return avklaringsbehovene
    }

    override fun alleEkskludertVentebehov(): List<Avklaringsbehov> {
        return avklaringsbehovene.filterNot { it.definisjon.erVentebehov() }
    }

    fun åpne(): List<Avklaringsbehov> {
        return alle().filter { it.erÅpent() }.toList()
    }

    fun skalTilbakeføresEtterTotrinnsVurdering(): Boolean {
        return tilbakeførtFraBeslutter().isNotEmpty()
    }

    override fun skalTilbakeføresEtterKvalitetssikring(): Boolean {
        return tilbakeførtFraKvalitetssikrer().isNotEmpty()
    }

    fun tilbakeførtFraBeslutter(): List<Avklaringsbehov> {
        return alle().filter { it.status() == Status.SENDT_TILBAKE_FRA_BESLUTTER }.toList()
    }

    fun tilbakeførtFraKvalitetssikrer(): List<Avklaringsbehov> {
        return alle().filter { it.status() == Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER }.toList()
    }

    override fun hentBehovForDefinisjon(definisjon: Definisjon): Avklaringsbehov? {
        return alle().singleOrNull { it.definisjon == definisjon }
    }

    fun hentBehovForDefinisjon(definisjoner: List<Definisjon>): List<Avklaringsbehov> {
        return alle().filter { it.definisjon in definisjoner }.toList()
    }

    fun avklaringsbehovLøstAvNay(): List<Avklaringsbehov> {
        return alle().filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
            .filter { it.definisjon.løsesAv == listOf(Rolle.SAKSBEHANDLER_NASJONAL) }
            .filterNot { it.erForeslåttVedtak() }
    }

    fun harAvklaringsbehovSomKreverToTrinn(): Boolean {
        return alle().any { it.erIkkeAvbrutt() && it.erTotrinn() }
    }

    fun harHattAvklaringsbehovSomHarKrevdToTrinn(): Boolean {
        return alle()
            .filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
            .any { avklaringsbehov -> avklaringsbehov.erTotrinn() && !avklaringsbehov.erTotrinnsVurdert() }
    }

    fun harAvklaringsbehovSomKreverToTrinnMenIkkeErVurdert(): Boolean {
        return alle().any { it.erIkkeAvbrutt() && it.erTotrinn() && !it.erTotrinnsVurdert() }
    }

    fun harHattAvklaringsbehovSomKreverKvalitetssikring(): Boolean {
        return alle()
            .filter { avklaringsbehov -> avklaringsbehov.kreverKvalitetssikring() }
            .filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
            .any { avklaringsbehov -> !avklaringsbehov.erKvalitetssikretTidligere() }
    }

    fun harAvklaringsbehovSomKreverKvalitetssikring(): Boolean {
        return alle()
            .filter { avklaringsbehov -> avklaringsbehov.kreverKvalitetssikring() }
            .any { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
    }

    fun harVærtSendtTilbakeFraBeslutterTidligere(): Boolean {
        return alle().any { avklaringsbehov -> avklaringsbehov.harVærtSendtTilbakeFraBeslutterTidligere() }
    }

    fun hentNyesteKvalitetssikringGittDefinisjon(definisjon: Definisjon): Endring? {
        return hentBehovForDefinisjon(definisjon)?.historikk?.filter { it.status == Status.KVALITETSSIKRET }
            ?.maxOrNull()
    }

    fun beslutningFor(definisjon: Definisjon): Endring? {
        return hentBehovForDefinisjon(definisjon)?.historikk?.filter { it.status == Status.TOTRINNS_VURDERT }
            ?.maxOrNull()
    }

    fun validateTilstand(behandling: Behandling, avklaringsbehov: Definisjon? = null) {
        ValiderBehandlingTilstand.validerTilstandBehandling(
            behandling = behandling,
            avklaringsbehov = avklaringsbehov,
            eksisterendeAvklaringsbehov = avklaringsbehovene
        )
    }

    fun validerPerioder(løsning: AvklaringsbehovLøsning) {
        if (løsning is PeriodisertAvklaringsbehovLøsning<*>) {
            val perioderDekketAvLøsning = løsning.løsningerForPerioder.sortedBy { it.fom }
                .somTidslinje { Periode(fom = it.fom, tom = it.tom ?: Tid.MAKS) }.map { true }.komprimer()

            val behovForDefinisjon = this.hentBehovForDefinisjon(løsning.definisjon())
            if (behovForDefinisjon != null) {
                val perioderSomSkalLøses = behovForDefinisjon.perioderSomSkalLøses().orEmpty().somTidslinje { it }

                val perioderSomManglerLøsning = perioderSomSkalLøses.leftJoin(perioderDekketAvLøsning) { _, periodeILøsning -> 
                    periodeILøsning != null
                }.filter{!it.verdi}.perioder().toSet()
                
                if (perioderSomManglerLøsning.isNotEmpty()) {
                    throw UgyldigForespørselException("Løsning mangler vurdering for perioder: $perioderSomManglerLøsning")
                }
            }
        }
    }

    fun validerPlassering(behandling: Behandling) {
        val nesteSteg = behandling.aktivtSteg()
        val behandlingFlyt = behandling.flyt()
        behandlingFlyt.forberedFlyt(nesteSteg)
        val uhåndterteBehov = alle().filter { it.erÅpent() }
            .filter { definisjon ->
                behandlingFlyt.erStegFør(
                    definisjon.løsesISteg(),
                    nesteSteg
                )
            }
        if (uhåndterteBehov.isNotEmpty()) {
            throw IllegalStateException("Har uhåndterte behov som skulle vært håndtert før nåværende steg = '$nesteSteg'. Behov: ${uhåndterteBehov.map { it.definisjon }}")
        }
    }

    override fun erSattPåVent(): Boolean {
        return alle().any { avklaringsbehov -> avklaringsbehov.erVentepunkt() && avklaringsbehov.erÅpent() }
    }

    fun hentÅpneVentebehov(): List<Avklaringsbehov> {
        return alle().filter { it.erVentepunkt() && it.erÅpent() }
    }

    override fun harÅpentBrevVentebehov(): Boolean {
        return alle().any { avklaringsbehov -> avklaringsbehov.erBrevVentebehov() && avklaringsbehov.erÅpent() }
    }

    override fun erVurdertTidligereIBehandlingen(definisjon: Definisjon): Boolean {
        val avklaringsbehov = hentBehovForDefinisjon(definisjon)
        return avklaringsbehov != null && avklaringsbehov.harAvsluttetStatusIHistorikken()
    }

    override fun toString(): String {
        return "Behov[${avklaringsbehovene.joinToString { it.toString() }}. For ID $behandlingId.]"
    }
}