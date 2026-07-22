package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.tilgang.Rolle
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class Avklaringsbehovene(
    private val repository: AvklaringsbehovOperasjonerRepository,
    private val behandlingId: BehandlingId,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val avklaringsbehovene: List<Avklaringsbehov>
        get() = repository.hent(behandlingId)

    fun løsAvklaringsbehov(
        definisjon: Definisjon,
        begrunnelse: String,
        endretAv: Bruker,
        kreverToTrinn: Boolean? = null
    ) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        if (kreverToTrinn == null) {
            avklaringsbehov.løs(begrunnelse, endretAv = endretAv)
        } else {
            avklaringsbehov.løs(begrunnelse = begrunnelse, endretAv = endretAv, kreverToTrinn = kreverToTrinn)
            repository.kreverToTrinn(avklaringsbehov.id, kreverToTrinn)
        }
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk().last())
    }

    fun leggTilFrivilligHvisMangler(definisjon: Definisjon, bruker: Bruker) {
        if (definisjon.erFrivillig()) {
            if (hentBehovForDefinisjon(definisjon) == null) {
                // Legger til frivillig behov
                leggTil(
                    definisjon = definisjon,
                    funnetISteg = definisjon.løsesISteg,
                    bruker = bruker,
                    perioderVedtaketBehøverVurdering = null,
                    perioderSomIkkeErTilstrekkeligVurdert = null
                )
            }
        }
    }

    fun leggTilOverstyringHvisMangler(definisjon: Definisjon, bruker: Bruker) {
        if (definisjon.erOverstyring()) {
            if (hentBehovForDefinisjon(definisjon) == null) {
                leggTil(
                    definisjon = definisjon,
                    funnetISteg = definisjon.løsesISteg,
                    bruker = bruker,
                    perioderVedtaketBehøverVurdering = null,
                    perioderSomIkkeErTilstrekkeligVurdert = null
                )
            }
        }
    }

    /**
     * Legger til nye avklaringsbehov.
     *
     * NB! Dersom avklaringsbehovet finnes fra før og er åpent så ignorerer vi det nye behovet, mens dersom det er avsluttet eller avbrutt så reåpner vi det.
     */
    fun leggTil(
        definisjon: Definisjon,
        funnetISteg: StegType,
        perioderSomIkkeErTilstrekkeligVurdert: Set<Periode>?,
        perioderVedtaketBehøverVurdering: Set<Periode>?,
        frist: LocalDate? = null,
        begrunnelse: String = "",
        grunn: ÅrsakTilSettPåVent? = null,
        bruker: Bruker = SYSTEMBRUKER
    ) {
        log.info("Legger til avklaringsbehov :: {} - {}", definisjon, funnetISteg)
        val avklaringsbehov = hentBehovForDefinisjon(definisjon)
        if (avklaringsbehov != null) {
            if (avklaringsbehov.erAvsluttet()) {
                avklaringsbehov.reåpne(
                    frist = utledFrist(definisjon, frist),
                    begrunnelse = begrunnelse,
                    venteårsak = grunn,
                    bruker = bruker,
                    perioderVedtaketBehøverVurdering = perioderVedtaketBehøverVurdering,
                    perioderSomIkkeErTilstrekkeligVurdert = perioderSomIkkeErTilstrekkeligVurdert
                )
                if (avklaringsbehov.erVentepunkt() || avklaringsbehov.erAutomatisk()) {
                    // TODO: Vurdere om funnet steg bør ligge på endringen...
                    repository.endreVentepunkt(avklaringsbehov.id, avklaringsbehov.historikk().last(), funnetISteg)
                } else {
                    repository.endre(avklaringsbehov.id, avklaringsbehov.historikk().last())
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
                endretAv = bruker
            )
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
        vurdertAv: Bruker,
        årsakTilRetur: List<ÅrsakTilRetur> = emptyList(),
    ) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.vurderTotrinn(begrunnelse, godkjent, vurdertAv, årsakTilRetur)
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk().last())
    }

    fun vurderKvalitet(
        definisjon: Definisjon,
        godkjent: Boolean,
        begrunnelse: String,
        vurdertAv: Bruker,
        årsakTilRetur: List<ÅrsakTilRetur> = emptyList(),
    ) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.vurderKvalitet(begrunnelse, godkjent, vurdertAv, årsakTilRetur)
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk().last())
    }

    internal fun avbryt(definisjon: Definisjon) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.avbryt()
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk().last())
    }

    /**
     *  Brukes for logikk internt for avklaringsbehov. Fra steg, bruk AvklaringsbehovService / løs-metoden.
     */
    internal fun avslutt(definisjon: Definisjon, begrunnelse: String) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        avklaringsbehov.avslutt(begrunnelse)
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk().last())
    }


    fun reåpne(
        definisjon: Definisjon
    ) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        val frist = if (definisjon.erVentebehov()) {
            avklaringsbehov.frist()
        } else {
            null
        }
        avklaringsbehov.reåpne(
            frist = frist,
            venteårsak = avklaringsbehov.venteårsak(),
            perioderSomIkkeErTilstrekkeligVurdert = null, // Kan ikke si noe om dette
            perioderVedtaketBehøverVurdering = avklaringsbehov.perioderVedtaketBehøverVurdering()
        )
        repository.endre(avklaringsbehov.id, avklaringsbehov.historikk().last())
    }

    internal fun oppdaterPerioder(
        definisjon: Definisjon,
        perioderSomIkkeErTilstrekkeligVurdert: Set<Periode>?,
        perioderVedtaketBehøverVurdering: Set<Periode>?
    ) {
        val avklaringsbehov = alle().single { it.definisjon == definisjon }
        val harEndring = avklaringsbehov.oppdaterPerioder(
            perioderSomIkkeErTilstrekkeligVurdert = perioderSomIkkeErTilstrekkeligVurdert,
            perioderVedtaketBehøverVurdering = perioderVedtaketBehøverVurdering
        )
        if (harEndring) {
            repository.endre(avklaringsbehov.id, avklaringsbehov.historikk().last())
        }
    }


    fun alle(): List<Avklaringsbehov> {
        return avklaringsbehovene
    }

    fun alleEkskludertAvbruttOgVentebehov(): List<Avklaringsbehov> {
        return avklaringsbehovene
            .filterNot { it.status() == Status.AVBRUTT || it.definisjon.erVentebehov() }
    }

    fun åpne(): List<Avklaringsbehov> {
        return alle().filter { it.erÅpent() }.toList()
    }

    fun skalTilbakeføresEtterKvalitetssikring(): Boolean {
        return tilbakeførtFraKvalitetssikrer().isNotEmpty()
    }

    fun tilbakeførtFraKvalitetssikrer(): List<Avklaringsbehov> {
        return alle().filter { it.status() == Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER }.toList()
    }

    fun hentBehovForDefinisjon(definisjon: Definisjon): Avklaringsbehov? {
        return alle().singleOrNull { it.definisjon == definisjon }
    }

    fun hentBehovForDefinisjon(definisjoner: List<Definisjon>): List<Avklaringsbehov> {
        return alle().filter { it.definisjon in definisjoner }.toList()
    }

    fun harVærtInnomSykdom(): Boolean {
        return alle().filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
            .any { it.definisjon == Definisjon.AVKLAR_SYKDOM }
    }

    fun avklaringsbehovLøstAvNay(): List<Avklaringsbehov> {
        return alle().filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
            .filter { it.definisjon.løsesAv == listOf(Rolle.SAKSBEHANDLER_NASJONAL) }
            .filterNot { it.erForeslåttVedtak() || it.erForeslåttVedtakVedtakslengde() }
    }

    fun harAvklaringsbehovSomKreverToTrinn(): Boolean {
        return alle().any { it.erIkkeAvbrutt() && it.erTotrinn() }
    }

    fun harAvklaringsbehovSomKreverToTrinnMenIkkeErGodkjent(): Boolean {
        return alle().any { it.erIkkeAvbrutt() && it.erTotrinn() && !it.erTotrinnsVurdert() }
    }

    fun harAvklaringsbehovSomKreverKvalitetssikring(): Boolean {
        return alle()
            .filter { avklaringsbehov -> avklaringsbehov.kreverKvalitetssikring() }
            .any { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
    }

    fun harAvklaringsbehovSomKreverKvalitetssikringMenIkkeErGodkjent(): Boolean {
        return alle().any { it.erIkkeAvbrutt() && it.kreverKvalitetssikring() && !it.erKvalitetssikret() }
    }

    fun harVærtSendtTilbakeFraBeslutterTidligere(): Boolean {
        return alle().any { avklaringsbehov -> avklaringsbehov.harVærtSendtTilbakeFraBeslutterTidligere() }
    }

    fun erLøstAv(bruker: Bruker, definisjon: Definisjon): Boolean {
        return bruker in hentBehovForDefinisjon(definisjon)?.brukere().orEmpty()
    }

    fun hentNyesteKvalitetssikringGittDefinisjon(definisjon: Definisjon): Endring? {
        return hentBehovForDefinisjon(definisjon)?.historikk()?.filter {
            it.status in setOf(
                Status.KVALITETSSIKRET,
                Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER
            )
        }?.maxByOrNull { it.tidsstempel }
    }

    fun hentNyesteBeslutningGittDefinisjon(definisjon: Definisjon): Endring? {
        return hentBehovForDefinisjon(definisjon)?.historikk()?.filter {
            it.status in setOf(
                Status.TOTRINNS_VURDERT,
                Status.SENDT_TILBAKE_FRA_BESLUTTER
            )
        }?.maxByOrNull { it.tidsstempel }
    }

    fun validerTilstand(behandling: Behandling, avklaringsbehov: Definisjon? = null) {
        ValiderBehandlingTilstand.validerTilstandBehandling(
            behandling = behandling,
            avklaringsbehov = avklaringsbehov,
            eksisterendeAvklaringsbehov = avklaringsbehovene
        )
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

    fun erSattPåVent(): Boolean {
        return alle().any { avklaringsbehov -> avklaringsbehov.erVentepunkt() && avklaringsbehov.erÅpent() }
    }

    fun hentÅpneVentebehov(): List<Avklaringsbehov> {
        return alle().filter { it.erVentepunkt() && it.erÅpent() }
    }

    fun erVurdertTidligereIBehandlingen(definisjon: Definisjon): Boolean {
        val avklaringsbehov = hentBehovForDefinisjon(definisjon)
        return avklaringsbehov != null && avklaringsbehov.harAvsluttetStatusIHistorikken()
    }

    override fun toString(): String {
        return "Behov[${avklaringsbehovene.joinToString { it.toString() }}. For ID $behandlingId.]"
    }

    fun allePlussFrivillige(behandling: Behandling): List<Avklaringsbehov> {
        val eksisterendeBehov = alle()
        val flyt = behandling.flyt()
        val list = flyt.frivilligeAvklaringsbehovRelevantForFlyten(behandling.aktivtSteg())
            .filter { definisjon -> eksisterendeBehov.none { behov -> behov.definisjon == definisjon } }
            .map { definisjon ->
                Avklaringsbehov(
                    id = Long.MAX_VALUE,
                    definisjon = definisjon,
                    historikk = mutableListOf(
                        Endring(
                            status = Status.OPPRETTET,
                            tidsstempel = LocalDateTime.now(),
                            begrunnelse = "",
                            endretAv = SYSTEMBRUKER
                        )
                    ),
                    funnetISteg = definisjon.løsesISteg,
                    kreverToTrinn = null
                )
            }.toMutableList()
        list.addAll(eksisterendeBehov)

        return list.sortedWith(flyt.avklaringsbehovComparator)
    }

    fun erÅpent(definisjon: Definisjon): Boolean {
        return hentBehovForDefinisjon(definisjon)?.erÅpent() == true
    }
}
