package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegOrkestrator
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
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

        var gjeldendeSteg = behandlingFlyt.forberedFlyt(behandling.aktivtSteg())

        while (true) {
            val avklaringsbehov = behandling.avklaringsbehov().filter { behov -> behov.erÅpent() }
            validerPlassering(
                behandlingFlyt,
                avklaringsbehov.filter { it.status() != Status.SENDT_TILBAKE_FRA_BESLUTTER }
                    .map { behov -> behov.definisjon },
                gjeldendeSteg.type()
            )

            val result = StegOrkestrator(gjeldendeSteg).utfør(
                kontekst,
                avklaringsbehov,
                behandling
            )

            if (result.erTilbakeføring()) {
                val tilbakeføringsflyt =
                    behandlingFlyt.tilbakeflyt(behandling.avklaringsbehovene().tilbakeførtFraBeslutter())
                log.info(
                    "[{} - {}] Tilakeført fra '{}' til '{}'",
                    kontekst.sakId,
                    kontekst.behandlingId,
                    gjeldendeSteg.type(),
                    tilbakeføringsflyt.stegene().last()
                )
                tilbakefør(kontekst, behandling, tilbakeføringsflyt)
            }

            val neste = behandlingFlyt.neste()

            if (result.kanFortsette() && neste != null) {
                gjeldendeSteg = neste
            } else {
                // Prosessen har stoppet opp, slipp ut hendelse om at den har stoppet opp og hvorfor?
                loggStopp(kontekst, behandling)
                return
            }
        }
    }

    internal fun forberedLøsingAvBehov(definisjoner: List<Definisjon>, behandling: Behandling, kontekst: FlytKontekst) {

        val behovForLøsninger = behandling.avklaringsbehovene().hentBehovForLøsninger(definisjoner)

        val tilbakeføringsflyt = behandling.flyt().tilbakeflyt(behovForLøsninger)

        tilbakefør(kontekst, behandling, tilbakeføringsflyt)
    }

    private fun tilbakefør(
        kontekst: FlytKontekst,
        behandling: Behandling,
        behandlingFlyt: BehandlingFlyt
    ) {
        var neste: BehandlingSteg?

        var kanFortsette = !behandlingFlyt.erTom()
        while (kanFortsette) {
            neste = behandlingFlyt.neste()

            if (neste == null) {
                kanFortsette = false
            } else {
                StegOrkestrator(neste).utførTilbakefør(
                    kontekst = kontekst,
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
            behandling.aktivtSteg(),
            behandling.åpneAvklaringsbehov()
        )
    }

    private fun validerPlassering(
        behandlingFlyt: BehandlingFlyt,
        åpneAvklaringsbehov: List<Definisjon>,
        nesteSteg: StegType
    ) {
        val uhåndterteBehov = åpneAvklaringsbehov
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
