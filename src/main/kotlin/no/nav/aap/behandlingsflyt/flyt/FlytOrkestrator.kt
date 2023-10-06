package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegOrkestrator
import no.nav.aap.behandlingsflyt.flyt.steg.StegStatus
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.Tilstand
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(FlytOrkestrator::class.java)


/**
 * Har ansvar for å drive flyten til en gitt behandling. Typen behandling styrer hvilke steg som skal utføres.
 */
class FlytOrkestrator {

    fun prosesserBehandling(kontekst: FlytKontekst) {
        val behandling = BehandlingTjeneste.hent(kontekst.behandlingId)

        ValiderBehandlingTilstand.validerTilstandBehandling(behandling, listOf())

        val behandlingFlyt = behandling.flyt()

        var aktivtSteg = behandling.aktivtSteg()
        behandlingFlyt.forberedFlyt(aktivtSteg.tilstand.steg())
        var nesteStegStatus = aktivtSteg.tilstand.status()
        var nesteSteg = behandlingFlyt.steg(aktivtSteg.tilstand.steg())

        var kanFortsette = true
        while (kanFortsette) {
            val avklaringsbehov = behandling.avklaringsbehov().filter { behov -> behov.erÅpent() }
            validerPlassering(
                behandlingFlyt,
                avklaringsbehov.filter { it.status() != Status.SENDT_TILBAKE_FRA_BESLUTTER }
                    .map { behov -> behov.definisjon },
                nesteSteg.type()
            )

            val result = StegOrkestrator(nesteSteg).utførTilstandsEndring(
                kontekst,
                nesteStegStatus,
                avklaringsbehov,
                behandling
            )

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
                val tilSteg = behandlingFlyt.finnTidligsteVedTilbakeføring(behandling.avklaringsbehovene())
                log.info(
                    "[{} - {}] Tilakeført fra '{}' til '{}'",
                    kontekst.sakId,
                    kontekst.behandlingId,
                    aktivtSteg.tilstand,
                    tilSteg
                )
                hoppTilbakeTilSteg(kontekst, behandling, tilSteg)
                aktivtSteg = behandling.aktivtSteg()
            }

            val neste =
                if (aktivtSteg.tilstand.status() == StegStatus.AVSLUTTER && nesteStegStatus == StegStatus.START) {
                    behandlingFlyt.neste()
                } else {
                    nesteSteg
                }

            kanFortsette = result.kanFortsette() && neste != null

            if (kanFortsette) {
                aktivtSteg = behandling.aktivtSteg()
                nesteStegStatus = aktivtSteg.utledNesteStegStatus()
                nesteSteg = neste!!
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

            hoppTilbakeTilSteg(kontekst, behandling, tilSteg)
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
        tilSteg: StegType
    ) {
        val behandlingFlyt = behandling.flyt()

        var forrige: BehandlingSteg?

        var kanFortsette = true
        while (kanFortsette) {
            forrige = behandlingFlyt.forrige()

            // TODO: Refactor
            if (forrige == null) {
                kanFortsette = false
            } else {
                var status = StegStatus.TILBAKEFØRT
                if (forrige.type() == tilSteg) {
                    status = StegStatus.START
                    kanFortsette = false
                }
                StegOrkestrator(forrige).utførTilstandsEndring(
                    kontekst = kontekst,
                    nesteStegStatus = status,
                    avklaringsbehov = listOf(),
                    behandling = behandling
                )
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
        nesteSteg: StegType
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
                )
            }
        if (uhåndterteBehov.isNotEmpty()) {
            throw IllegalStateException("Har uhåndterte behov som skulle vært håndtert før nåværende steg = '$nesteSteg'")
        }
    }

    fun settBehandlingPåVent(kontekst: FlytKontekst) {
        val behandling = BehandlingTjeneste.hent(kontekst.behandlingId)
        behandling.settPåVent()
    }
}
