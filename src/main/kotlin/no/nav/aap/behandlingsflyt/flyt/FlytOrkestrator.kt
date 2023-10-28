package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.flyt.internal.FlytOperasjonRepository
import no.nav.aap.behandlingsflyt.flyt.steg.StegOrkestrator
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(FlytOrkestrator::class.java)


/**
 * Har ansvar for å drive flyten til en gitt behandling. Typen behandling styrer hvilke steg som skal utføres.
 */
class FlytOrkestrator(
    private val connection: DBConnection
) {

    private val faktagrunnlag = Faktagrunnlag(connection)
    private val flytOperasjonRepository = FlytOperasjonRepository(connection)

    fun forberedBehandling(kontekst: FlytKontekst) {
        val behandling = BehandlingRepository(connection).hent(kontekst.behandlingId)

        ValiderBehandlingTilstand.validerTilstandBehandling(behandling, listOf())

        val behandlingFlyt = behandling.flyt()
        behandlingFlyt.forberedFlyt(behandling.aktivtSteg())

        if (starterOppBehandling(behandling)) {
            flytOperasjonRepository.oppdaterSakStatus(kontekst.sakId, no.nav.aap.behandlingsflyt.sak.Status.UTREDES)
        }

        val oppdaterFaktagrunnlagForKravliste =
            faktagrunnlag.oppdaterFaktagrunnlagForKravliste(
                behandlingFlyt.faktagrunnlagFremTilGjeldendeSteg(),
                kontekst = kontekst
            )

        val tilbakeføringsflyt = behandlingFlyt.tilbakeflytEtterEndringer(oppdaterFaktagrunnlagForKravliste)

        if (!tilbakeføringsflyt.erTom()) {
            log.info(
                "[{} - {}] Tilakeført etter oppdatering av registeropplysninger fra '{}' til '{}'",
                kontekst.sakId,
                kontekst.behandlingId,
                behandling.aktivtSteg(),
                tilbakeføringsflyt.stegene().last()
            )
        }
        tilbakefør(kontekst, behandling, tilbakeføringsflyt)
    }

    private fun starterOppBehandling(behandling: Behandling): Boolean {
        return behandling.stegHistorikk().isEmpty()
    }

    fun prosesserBehandling(kontekst: FlytKontekst) {
        val behandling = BehandlingRepository(connection).hent(kontekst.behandlingId)

        ValiderBehandlingTilstand.validerTilstandBehandling(behandling, listOf())

        val behandlingFlyt = behandling.flyt()

        var gjeldendeSteg = behandlingFlyt.forberedFlyt(behandling.aktivtSteg())

        while (true) {
            connection.markerSavepoint()

            val avklaringsbehov = behandling.avklaringsbehovene().åpne()
            validerPlassering(
                behandlingFlyt,
                avklaringsbehov
                    .filter { it.status() != Status.SENDT_TILBAKE_FRA_BESLUTTER }
                    .map { behov -> behov.definisjon },
                gjeldendeSteg.type()
            )

            faktagrunnlag.oppdaterFaktagrunnlagForKravliste(
                behandlingFlyt.faktagrunnlagForGjeldendeSteg(),
                kontekst
            )

            connection.markerSavepoint()

            val result = StegOrkestrator(connection, gjeldendeSteg).utfør(kontekst, behandling)

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

            if (!result.kanFortsette() || neste == null) {
                // Prosessen har stoppet opp, slipp ut hendelse om at den har stoppet opp og hvorfor?
                loggStopp(kontekst, behandling)
                return
            }
            gjeldendeSteg = neste
        }
    }

    internal fun forberedLøsingAvBehov(definisjoner: List<Definisjon>, behandling: Behandling, kontekst: FlytKontekst) {

        val behovForLøsninger = behandling.avklaringsbehovene().hentBehovForDefinisjon(definisjoner)

        val tilbakeføringsflyt = behandling.flyt().tilbakeflyt(behovForLøsninger)

        tilbakefør(kontekst, behandling, tilbakeføringsflyt)
    }

    private fun tilbakefør(
        kontekst: FlytKontekst,
        behandling: Behandling,
        behandlingFlyt: BehandlingFlyt
    ) {
        if (behandlingFlyt.erTom()) {
            return
        }

        while (true) {
            val neste = behandlingFlyt.neste()

            if (neste == null) {
                loggStopp(kontekst, behandling)
                return
            }
            StegOrkestrator(connection, neste).utførTilbakefør(
                kontekst = kontekst,
                behandling = behandling
            )
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
        val behandling = BehandlingRepository(connection).hent(kontekst.behandlingId)
        behandling.settPåVent()
        flytOperasjonRepository.leggTilAvklaringsbehov(behandling.id, Definisjon.MANUELT_SATT_PÅ_VENT, behandling.aktivtSteg())
    }
}
