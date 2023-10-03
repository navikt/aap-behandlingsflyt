package no.nav.aap.behandlingsflyt.flyt.kontroll

import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.StegStatus
import no.nav.aap.behandlingsflyt.flyt.StegType
import no.nav.aap.behandlingsflyt.flyt.Tilstand
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(FlytKontroller::class.java)


/**
 * Har ansvar for å drive flyten til en gitt behandling. Typen behandling styrer hvilke steg som skal utføres.
 */
class FlytKontroller {

    fun prosesserBehandling(kontekst: FlytKontekst) {
        val behandling = BehandlingTjeneste.hent(kontekst.behandlingId)

        ValiderBehandlingTilstand.validerTilstandBehandling(behandling, listOf())

        val behandlingFlyt = behandling.flyt()

        var aktivtSteg = behandling.aktivtSteg()
        var nesteStegStatus = aktivtSteg.tilstand.status()
        var nesteSteg = behandlingFlyt.steg(aktivtSteg.tilstand.steg())

        var kanFortsette = true
        while (kanFortsette) {
            val avklaringsbehov = behandling.avklaringsbehov().filter { behov -> behov.erÅpent() }
            validerPlassering(
                behandlingFlyt,
                avklaringsbehov.filter { it.status() != Status.SENDT_TILBAKE_FRA_BESLUTTER }
                    .map { behov -> behov.definisjon },
                nesteSteg.type(),
                nesteStegStatus
            )

            val result = utførTilstandsEndring(kontekst, nesteStegStatus, avklaringsbehov, nesteSteg, behandling)

            if (result.funnetAvklaringsbehov().isNotEmpty()) {
                log.info(
                    "[{} - {}] Fant avklaringsbehov: {}",
                    kontekst.sakId,
                    kontekst.behandlingId,
                    result.funnetAvklaringsbehov()
                )
                behandling.leggTil(result.funnetAvklaringsbehov())
            }

            if (result.erTilbakeføring()) {
                hoppTilbakeTilSteg(kontekst, behandling, result.tilSteg(), StegStatus.START)
                aktivtSteg = behandling.aktivtSteg()
            }

            kanFortsette = result.kanFortsette() && behandlingFlyt.utledNesteSteg(aktivtSteg, nesteStegStatus) != null

            if (kanFortsette) {
                aktivtSteg = behandling.aktivtSteg()
                nesteStegStatus = aktivtSteg.utledNesteStegStatus()
                nesteSteg = behandlingFlyt.utledNesteSteg(aktivtSteg, nesteStegStatus)!!
            } else {
                // Prosessen har stoppet opp, slipp ut hendelse om at den har stoppet opp og hvorfor?
                loggStopp(kontekst, behandling)
            }
        }
    }

    internal fun forberedLøsingAvBehov(definisjoner: List<Definisjon>, behandling: Behandling, kontekst: FlytKontekst) {
        if (behandling.skalHoppesTilbake(definisjoner)) {
            val tilSteg = utledSteg(
                behandling.flyt(),
                behandling.aktivtSteg(),
                behandling.avklaringsbehov()
                    .filter { behov -> definisjoner.any { it == behov.definisjon } })
            val tilStegStatus =
                utledStegStatus(definisjoner
                    .filter { it.løsesISteg == tilSteg }
                    .map { it.vurderingspunkt.stegStatus })

            hoppTilbakeTilSteg(kontekst, behandling, tilSteg, tilStegStatus)
        } else if (skalRekjøreSteg(definisjoner, behandling)) {
            flyttTilStartAvAktivtSteg(behandling)
        }
    }

    private fun skalRekjøreSteg(
        avklaringsbehov: List<Definisjon>,
        behandling: Behandling
    ): Boolean {
        return avklaringsbehov
            .filter { it.løsesISteg == behandling.aktivtSteg().tilstand.steg() }
            .any { it.rekjørSteg }
    }

    private fun hoppTilbakeTilSteg(
        kontekst: FlytKontekst,
        behandling: Behandling,
        tilSteg: StegType,
        tilStegStatus: StegStatus
    ) {
        val behandlingFlyt = behandling.type.flyt()

        val aktivtSteg = behandling.aktivtSteg()
        var forrige: BehandlingSteg? = behandlingFlyt.steg(aktivtSteg.tilstand.steg())

        var kanFortsette = true
        while (kanFortsette) {
            forrige = behandlingFlyt.forrige(forrige!!.type())

            // TODO: Refactor
            if (forrige == null) {
                kanFortsette = false
            } else if (forrige.type() == tilSteg && tilStegStatus != StegStatus.INNGANG) {
                utførTilstandsEndring(
                    kontekst = kontekst,
                    nesteStegStatus = tilStegStatus,
                    avklaringsbehov = listOf(),
                    aktivtSteg = forrige,
                    behandling = behandling
                )
                kanFortsette = false
            } else {
                utførTilstandsEndring(
                    kontekst = kontekst,
                    nesteStegStatus = StegStatus.TILBAKEFØRT,
                    avklaringsbehov = listOf(),
                    aktivtSteg = forrige,
                    behandling = behandling
                )
            }
            if (kanFortsette && forrige!!.type() == tilSteg && tilStegStatus == StegStatus.INNGANG) {
                kanFortsette = false
            }
            if (!kanFortsette) {
                loggStopp(kontekst, behandling)
            }
        }
    }

    private fun loggStopp(
        kontekst: FlytKontekst,
        behandling: Behandling
    ) {
        log.info(
            "[{} - {}] Stopper opp ved {} med {}",
            kontekst.sakId,
            kontekst.behandlingId,
            behandling.aktivtSteg().tilstand,
            behandling.åpneAvklaringsbehov()
        )
    }

    private fun flyttTilStartAvAktivtSteg(behandling: Behandling) {
        val nyStegTilstand =
            StegTilstand(tilstand = Tilstand(behandling.aktivtSteg().tilstand.steg(), StegStatus.START))
        behandling.visit(nyStegTilstand)
    }

    private fun utledStegStatus(stegStatuser: List<StegStatus>): StegStatus {
        if (stegStatuser.contains(StegStatus.UTGANG)) {
            return StegStatus.UTGANG
        }
        return StegStatus.INNGANG
    }

    private fun utledSteg(
        behandlingFlyt: BehandlingFlyt,
        aktivtSteg: StegTilstand,
        avklaringsDefinisjoner: List<Avklaringsbehov>
    ): StegType {
        return avklaringsDefinisjoner.filter { definisjon ->
            erStegFørAktivtSteg(behandlingFlyt, aktivtSteg, definisjon.løsesISteg())
        }
            .map { definisjon -> definisjon.løsesISteg() }
            .minWith(behandlingFlyt.compareable())
    }

    private fun erStegFørAktivtSteg(
        behandlingFlyt: BehandlingFlyt,
        aktivtSteg: StegTilstand,
        løsesISteg: StegType
    ) = behandlingFlyt.erStegFør(
        løsesISteg,
        aktivtSteg.tilstand.steg()
    )

    private fun validerPlassering(
        behandlingFlyt: BehandlingFlyt,
        åpneAvklaringsbehov: List<Definisjon>,
        nesteSteg: StegType,
        nesteStegStatus: StegStatus
    ) {
        val uhåndterteBehov = åpneAvklaringsbehov
            .filter { definisjon ->
                behandlingFlyt.erStegFørEllerLik(
                    definisjon.løsesISteg,
                    nesteSteg
                )
            }
            .filter { definisjon ->
                behandlingFlyt.erStegFør(
                    definisjon.løsesISteg,
                    nesteSteg
                ) || definisjon.vurderingspunkt.stegStatus.erFør(nesteStegStatus)
            }
        if (uhåndterteBehov.isNotEmpty()) {
            throw IllegalStateException("Har uhåndterte behov som skulle vært håndtert før nåværende steg = '$nesteSteg' med status = '$nesteStegStatus'")
        }
    }

    private fun utførTilstandsEndring(
        kontekst: FlytKontekst,
        nesteStegStatus: StegStatus,
        avklaringsbehov: List<Avklaringsbehov>,
        aktivtSteg: BehandlingSteg,
        behandling: Behandling
    ): Transisjon {
        val relevanteAvklaringsbehov =
            avklaringsbehov.filter { behov -> behov.skalLøsesISteg(aktivtSteg.type()) }

        log.debug(
            "[{} - {}] Behandler steg({}) med status({})",
            kontekst.sakId,
            kontekst.behandlingId,
            aktivtSteg.type(),
            nesteStegStatus
        )
        val transisjon = when (nesteStegStatus) {
            StegStatus.INNGANG -> harAvklaringspunkt(aktivtSteg.type(), nesteStegStatus, relevanteAvklaringsbehov)
            StegStatus.UTFØRER -> behandleSteg(aktivtSteg, kontekst)
            StegStatus.UTGANG -> harAvklaringspunkt(aktivtSteg.type(), nesteStegStatus, relevanteAvklaringsbehov)
            StegStatus.AVSLUTTER -> harTruffetSlutten(aktivtSteg.type(), behandling.flyt())
            StegStatus.TILBAKEFØRT -> behandleStegBakover(aktivtSteg, kontekst)
            else -> Fortsett
        }

        val nyStegTilstand = StegTilstand(tilstand = Tilstand(aktivtSteg.type(), nesteStegStatus))
        behandling.visit(nyStegTilstand)

        return transisjon
    }

    private fun behandleStegBakover(steg: BehandlingSteg, kontekst: FlytKontekst): Transisjon {
        val input = StegInput(kontekst)
        steg.vedTilbakeføring(input)

        return Fortsett
    }

    private fun harTruffetSlutten(aktivtSteg: StegType, flyt: BehandlingFlyt): Transisjon {
        return when (flyt.harTruffetSlutten(aktivtSteg)) {
            true -> Stopp
            else -> Fortsett
        }
    }

    private fun behandleSteg(steg: BehandlingSteg, kontekst: FlytKontekst): Transisjon {
        val input = StegInput(kontekst)
        val stegResultat = steg.utfør(input)

        return stegResultat.transisjon()
    }

    private fun harAvklaringspunkt(
        steg: StegType,
        nesteStegStatus: StegStatus,
        avklaringsbehov: List<Avklaringsbehov>
    ): Transisjon {

        if (avklaringsbehov.any { behov -> behov.skalStoppeHer(steg, nesteStegStatus) }) {
            return Stopp
        }

        return Fortsett
    }

    fun settBehandlingPåVent(kontekst: FlytKontekst) {
        val behandling = BehandlingTjeneste.hent(kontekst.behandlingId)
        behandling.settPåVent()
    }
}
