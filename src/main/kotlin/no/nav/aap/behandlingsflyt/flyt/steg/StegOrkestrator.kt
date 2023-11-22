package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(StegOrkestrator::class.java)

class StegOrkestrator(private val connection: DBConnection, private val aktivtSteg: FlytSteg) {

    private val behandlingRepository = BehandlingRepository(connection)
    private val avklaringsbehovRepository = AvklaringsbehovRepository(connection)

    fun utfør(
        kontekst: FlytKontekst,
        behandling: Behandling
    ): Transisjon {

        var gjeldendeStegStatus = StegStatus.START
        log.info("Behandler steg '{}'", aktivtSteg.type())

        while (true) {
            val resultat = utførTilstandsEndring(kontekst, gjeldendeStegStatus, behandling)

            if (resultat.funnetAvklaringsbehov().isNotEmpty()) {
                log.info(
                    "Fant avklaringsbehov: {}",
                    resultat.funnetAvklaringsbehov()
                )
                leggTilAvklaringsbehov(behandling, resultat)
            }

            if (!resultat.kanFortsette() || resultat.erTilbakeføring() || gjeldendeStegStatus == StegStatus.AVSLUTTER) {
                return resultat
            }
            gjeldendeStegStatus = gjeldendeStegStatus.neste()
        }
    }

    private fun leggTilAvklaringsbehov(
        behandling: Behandling,
        resultat: Transisjon
    ) {
        val definisjoner = resultat.funnetAvklaringsbehov()
        behandling.leggTil(definisjoner)
        //TODO: Skal dette gjøres utenfor stegene som her, eller i stegene?
        avklaringsbehovRepository.leggTilAvklaringsbehov(behandling.id, definisjoner, aktivtSteg.type())
    }

    fun utførTilbakefør(
        kontekst: FlytKontekst,
        behandling: Behandling
    ): Transisjon {
        return utførTilstandsEndring(kontekst, StegStatus.TILBAKEFØRT, behandling)
    }

    private fun utførTilstandsEndring(
        kontekst: FlytKontekst,
        nesteStegStatus: StegStatus,
        behandling: Behandling
    ): Transisjon {
        val relevanteAvklaringsbehov =
            avklaringsbehovRepository.hent(kontekst.behandlingId).alle()
                .filter { it.erÅpent() }
                .filter { behov -> behov.skalLøsesISteg(aktivtSteg.type()) }

        log.debug(
            "Behandler steg({}) med status({})",
            aktivtSteg.type(),
            nesteStegStatus
        )
        val transisjon = when (nesteStegStatus) {
            StegStatus.UTFØRER -> behandleSteg(aktivtSteg, kontekst)
            StegStatus.AVKLARINGSPUNKT -> harAvklaringspunkt(aktivtSteg.type(), relevanteAvklaringsbehov)
            StegStatus.AVSLUTTER -> Fortsett
            StegStatus.TILBAKEFØRT -> behandleStegBakover(aktivtSteg, kontekst)
            else -> Fortsett
        }

        val nyStegTilstand = StegTilstand(tilstand = Tilstand(aktivtSteg.type(), nesteStegStatus))
        loggStegHistorikk(behandling, nyStegTilstand)

        return transisjon
    }

    private fun loggStegHistorikk(
        behandling: Behandling,
        nyStegTilstand: StegTilstand
    ) {
        val førStatus = behandling.status()
        behandling.visit(nyStegTilstand)
        behandlingRepository.loggBesøktSteg(behandlingId = behandling.id, nyStegTilstand.tilstand)
        val etterStatus = nyStegTilstand.tilstand.steg().status
        if (førStatus != etterStatus) {
            behandlingRepository.oppdaterBehandlingStatus(behandlingId = behandling.id, status = etterStatus)
        }
    }

    private fun behandleStegBakover(flytSteg: FlytSteg, kontekst: FlytKontekst): Transisjon {
        val steg = flytSteg.konstruer(connection)
        steg.vedTilbakeføring(kontekst)

        return Fortsett
    }

    private fun behandleSteg(flytSteg: FlytSteg, kontekst: FlytKontekst): Transisjon {
        val steg = flytSteg.konstruer(connection)
        val stegResultat = steg.utfør(kontekst)

        return stegResultat.transisjon()
    }

    private fun harAvklaringspunkt(
        steg: StegType,
        avklaringsbehov: List<Avklaringsbehov>
    ): Transisjon {

        if (avklaringsbehov.any { behov -> behov.skalStoppeHer(steg) }) {
            return Stopp
        }

        return Fortsett
    }
}
