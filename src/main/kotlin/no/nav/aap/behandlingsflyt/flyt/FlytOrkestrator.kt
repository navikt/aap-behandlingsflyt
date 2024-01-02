package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.flyt.steg.StegOrkestrator
import no.nav.aap.behandlingsflyt.sak.SakFlytRepository
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(FlytOrkestrator::class.java)

/**
 * Har ansvar for å drive flyten til en gitt behandling. Typen behandling styrer hvilke steg som skal utføres.
 */
class FlytOrkestrator(
    private val connection: DBConnection
) {
    private val faktagrunnlag = Faktagrunnlag(connection)
    private val sakRepository = SakFlytRepository(connection)
    private val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)
    private val behandlingRepository = behandlingRepository(connection)

    fun forberedBehandling(kontekst: FlytKontekst) {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        ValiderBehandlingTilstand.validerTilstandBehandling(
            behandling = behandling,
            eksisterenedeAvklaringsbehov = avklaringsbehovene.alle()
        )

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
        tilbakefør(kontekst, behandling, tilbakeføringsflyt, avklaringsbehovene.åpne())
    }

    private fun starterOppBehandling(behandling: Behandling): Boolean {
        return behandling.stegHistorikk().isEmpty()
    }

    fun prosesserBehandling(kontekst: FlytKontekst) {
        val behandling = behandlingRepository(connection).hent(kontekst.behandlingId)
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        ValiderBehandlingTilstand.validerTilstandBehandling(
            behandling = behandling,
            eksisterenedeAvklaringsbehov = avklaringsbehovene.alle()
        )

        val behandlingFlyt = behandling.flyt()

        var gjeldendeSteg = behandlingFlyt.forberedFlyt(behandling.aktivtSteg())

        while (true) {
            connection.markerSavepoint()

            faktagrunnlag.oppdaterFaktagrunnlagForKravliste(
                behandlingFlyt.faktagrunnlagForGjeldendeSteg(),
                kontekst
            )

            connection.markerSavepoint()

            val result = StegOrkestrator(connection, gjeldendeSteg).utfør(kontekst, behandling)

            val avklaringsbehov = avklaringsbehovene.åpne()
            if (result.erTilbakeføring()) {
                val tilbakeføringsflyt = behandlingFlyt.tilbakeflyt(avklaringsbehovene.tilbakeførtFraBeslutter())
                log.info(
                    "Tilakeført fra '{}' til '{}'",
                    gjeldendeSteg.type(),
                    tilbakeføringsflyt.stegene().last()
                )
                tilbakefør(kontekst, behandling, tilbakeføringsflyt, avklaringsbehov)
            }
            validerPlassering(
                behandlingFlyt,
                avklaringsbehov.filter { it.status() != Status.SENDT_TILBAKE_FRA_BESLUTTER }
            )
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

    internal fun forberedLøsingAvBehov(definisjoner: Definisjon, behandling: Behandling, kontekst: FlytKontekst) {
        val flyt = behandling.forberedtFlyt()

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val behovForLøsninger = avklaringsbehovene.hentBehovForDefinisjon(definisjoner)
        val tilbakeføringsflyt = flyt.tilbakeflyt(behovForLøsninger)

        tilbakefør(kontekst, behandling, tilbakeføringsflyt, avklaringsbehovene.åpne())

        val skulleVærtISteg = flyt.skalTilStegForBehov(behovForLøsninger)
        if (skulleVærtISteg != null) {
            flyt.validerPlassering(skulleVærtISteg)
        }
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
        åpneAvklaringsbehov: List<Avklaringsbehov>
    ) {
        val nesteSteg = behandlingFlyt.aktivtStegType()
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
        //TODO: Vi må huske å lagre behandling etter at vi har endret status
        behandling.settPåVent()

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        avklaringsbehovene.leggTil(listOf(Definisjon.MANUELT_SATT_PÅ_VENT), behandling.aktivtSteg())
    }
}
