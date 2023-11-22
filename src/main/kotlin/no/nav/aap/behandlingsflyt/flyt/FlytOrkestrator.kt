package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.flyt.steg.StegOrkestrator
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.sak.SakRepository
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(FlytOrkestrator::class.java)

/**
 * Har ansvar for å drive flyten til en gitt behandling. Typen behandling styrer hvilke steg som skal utføres.
 */
class FlytOrkestrator(
    private val connection: DBConnection
) {
    private val faktagrunnlag = Faktagrunnlag(connection)
    private val sakRepository = SakRepository(connection)
    private val avklaringsbehovRepository = AvklaringsbehovRepository(connection)
    private val behandlingRepository = BehandlingRepository(connection)

    fun forberedBehandling(kontekst: FlytKontekst) {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        var avklaringsbehovene = avklaringsbehovRepository.hent(kontekst.behandlingId)

        ValiderBehandlingTilstand.validerTilstandBehandling(behandling, listOf(), avklaringsbehovene.alle())

        val behandlingFlyt = behandling.forberedtFlyt()

        if (starterOppBehandling(behandling)) {
            sakRepository.oppdaterSakStatus(kontekst.sakId, no.nav.aap.behandlingsflyt.sak.Status.UTREDES)
        }

        val oppdaterFaktagrunnlagForKravliste =
            faktagrunnlag.oppdaterFaktagrunnlagForKravliste(
                kravliste = behandlingFlyt.faktagrunnlagFremTilOgMedGjeldendeSteg(),
                kontekst = kontekst
            )

        val tilbakeføringsflyt = behandlingFlyt.tilbakeflytEtterEndringer(oppdaterFaktagrunnlagForKravliste)

        if (!tilbakeføringsflyt.erTom()) {
            log.info(
                "Tilbakeført etter oppdatering av registeropplysninger fra '{}' til '{}'",
                behandling.aktivtSteg(),
                tilbakeføringsflyt.stegene().last()
            )
        }
        //TODO: Må vi hente 2 ganger?
        avklaringsbehovene = avklaringsbehovRepository.hent(kontekst.behandlingId)
        tilbakefør(kontekst, behandling, tilbakeføringsflyt, avklaringsbehovene.åpne())
    }

    private fun starterOppBehandling(behandling: Behandling): Boolean {
        return behandling.stegHistorikk().isEmpty()
    }

    fun prosesserBehandling(kontekst: FlytKontekst) {
        val behandling = BehandlingRepository(connection).hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hent(kontekst.behandlingId)

        ValiderBehandlingTilstand.validerTilstandBehandling(behandling, listOf(), avklaringsbehovene.alle())

        val behandlingFlyt = behandling.flyt()

        var gjeldendeSteg = behandlingFlyt.forberedFlyt(behandling.aktivtSteg())

        while (true) {
            connection.markerSavepoint()

            val avklaringsbehov = avklaringsbehovene.åpne()
            validerPlassering(
                behandlingFlyt,
                avklaringsbehov.filter { it.status() != Status.SENDT_TILBAKE_FRA_BESLUTTER },
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
                    behandlingFlyt.tilbakeflyt(avklaringsbehovene.tilbakeførtFraBeslutter())
                log.info(
                    "Tilakeført fra '{}' til '{}'",
                    gjeldendeSteg.type(),
                    tilbakeføringsflyt.stegene().last()
                )
                tilbakefør(kontekst, behandling, tilbakeføringsflyt, avklaringsbehov)
            }

            val neste = behandlingFlyt.neste()

            if (!result.kanFortsette() || neste == null) {
                if (neste == null) {
                    // Avslutter behandling
                    log.info("Behandlingen har nådd slutten, avslutter behandling")
                } else {
                    // Prosessen har stoppet opp, slipp ut hendelse om at den har stoppet opp og hvorfor?
                    loggStopp(behandling, avklaringsbehovene.åpne())
                }
                return
            }
            gjeldendeSteg = neste
        }
    }

    internal fun forberedLøsingAvBehov(definisjoner: List<Definisjon>, behandling: Behandling, kontekst: FlytKontekst) {
        val flyt = behandling.forberedtFlyt()

        val avklaringsbehovene = avklaringsbehovRepository.hent(kontekst.behandlingId)
        val behovForLøsninger = avklaringsbehovene.hentBehovForDefinisjon(definisjoner)
        val tilbakeføringsflyt = flyt.tilbakeflyt(behovForLøsninger)

        tilbakefør(kontekst, behandling, tilbakeføringsflyt, avklaringsbehovene.åpne())

        val skulleVærtISteg = flyt.skalTilStegForBehov(behovForLøsninger)
        require(skulleVærtISteg == behandling.aktivtSteg())
    }

    private fun tilbakefør(
        kontekst: FlytKontekst,
        behandling: Behandling,
        behandlingFlyt: BehandlingFlyt,
        åpneAvklaringsbehov: List<Avklaringsbehov>
    ) {
        if (behandlingFlyt.erTom()) {
            return
        }

        while (true) {
            val neste = behandlingFlyt.neste()

            if (neste == null) {
                loggStopp(behandling, åpneAvklaringsbehov)
                return
            }
            StegOrkestrator(connection, neste).utførTilbakefør(
                kontekst = kontekst,
                behandling = behandling
            )
        }
    }

    private fun loggStopp(
        behandling: Behandling,
        åpneAvklaringsbehov: List<Avklaringsbehov>
    ) {
        log.info(
            "Stopper opp ved {} med {}",
            behandling.aktivtSteg(),
            åpneAvklaringsbehov
        )
    }

    private fun validerPlassering(
        behandlingFlyt: BehandlingFlyt,
        åpneAvklaringsbehov: List<Avklaringsbehov>,
        nesteSteg: StegType
    ) {
        val uhåndterteBehov = åpneAvklaringsbehov
            .filter { definisjon ->
                behandlingFlyt.erStegFør(
                    definisjon.løsesISteg(),
                    nesteSteg
                )
            }
        if (uhåndterteBehov.isNotEmpty()) {
            throw IllegalStateException("Har uhåndterte behov som skulle vært håndtert før nåværende steg = '$nesteSteg'")
        }
    }

    fun settBehandlingPåVent(kontekst: FlytKontekst) {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        behandling.settPåVent()
        //TODO: Vi må huske å lagre behandling etter at vi har endret status
        //TODO: settPåVent oppretter også avklaringsbehov som under - duplikat?
        avklaringsbehovRepository.leggTilAvklaringsbehov(
            behandling.id,
            Definisjon.MANUELT_SATT_PÅ_VENT,
            behandling.aktivtSteg()
        )
    }
}
