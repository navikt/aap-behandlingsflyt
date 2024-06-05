package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.periodisering.PerioderTilVurderingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingFlytRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegStatus
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(StegOrkestrator::class.java)

/**
 * Håndterer den definerte prosessen i et gitt steg, flytter behandlingen gjennom de forskjellige fasene internt i et
 * steg. Et steg beveger seg gjennom flere faser som har forskjellig ansvar.
 *
 * @see no.nav.aap.verdityper.flyt.StegStatus.START:            Teknisk markør for at flyten har flyttet seg til et gitt steg
 *
 * @see no.nav.aap.verdityper.flyt.StegStatus.UTFØRER:          Utfører forrettningslogikken i steget ved å kalle på
 * @see no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg#utfør()
 *
 * @see no.nav.aap.verdityper.flyt.StegStatus.AVKLARINGSPUNKT:  Vurderer om maskinen har bedt om besluttningstøtte fra
 * et menneske og stopper prosessen hvis det er et punkt som krever stopp i dette steget.
 *
 * @see no.nav.aap.verdityper.flyt.StegStatus.AVSLUTTER:        Teknisk markør for avslutting av steget
 */
class StegOrkestrator(private val connection: DBConnection, private val aktivtSteg: FlytSteg) {

    private val behandlingRepository = BehandlingFlytRepository(connection)
    private val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)
    private val perioderTilVurderingService = PerioderTilVurderingService(connection)

    private val behandlingSteg = aktivtSteg.konstruer(connection)

    fun utfør(
        kontekst: FlytKontekst,
        behandling: Behandling
    ): Transisjon {
        var gjeldendeStegStatus = StegStatus.START
        log.info("Behandler steg '{}'", aktivtSteg.type())

        while (true) {
            val resultat = utførTilstandsEndring(
                kontekst,
                gjeldendeStegStatus,
                behandling
            )
            if (gjeldendeStegStatus == StegStatus.START) {
                // Legger denne her slik at vi får savepoint på at vi har byttet steg, slik at vi starter opp igjen på rett sted når prosessen dras i gang igjen
                connection.markerSavepoint()
            }

            if (gjeldendeStegStatus == StegStatus.AVSLUTTER) {
                return resultat
            }

            if (!resultat.kanFortsette() || resultat.erTilbakeføring()) {
                return resultat
            }
            gjeldendeStegStatus = gjeldendeStegStatus.neste()
        }
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
        log.debug(
            "Behandler steg({}) med status({})",
            aktivtSteg.type(),
            nesteStegStatus
        )
        val transisjon = when (nesteStegStatus) {
            StegStatus.UTFØRER -> behandleSteg(kontekst)
            StegStatus.AVKLARINGSPUNKT -> harAvklaringspunkt(aktivtSteg.type(), kontekst.behandlingId)
            StegStatus.AVSLUTTER -> Fortsett
            StegStatus.TILBAKEFØRT -> behandleStegBakover(kontekst)
            else -> Fortsett
        }

        val nyStegTilstand = StegTilstand(stegType = aktivtSteg.type(), stegStatus = nesteStegStatus)
        loggStegHistorikk(behandling, nyStegTilstand)

        return transisjon
    }

    private fun behandleSteg(kontekst: FlytKontekst): Transisjon {
        val kontekstMedPerioder = FlytKontekstMedPerioder(
            sakId = kontekst.sakId,
            behandlingId = kontekst.behandlingId,
            behandlingType = kontekst.behandlingType,
            perioderTilVurdering = perioderTilVurderingService.utled(kontekst = kontekst, stegType = aktivtSteg.type())
        )
        val stegResultat = behandlingSteg.utfør(kontekstMedPerioder)

        val resultat = stegResultat.transisjon()

        if (resultat.funnetAvklaringsbehov().isNotEmpty()) {
            log.info(
                "Fant avklaringsbehov: {}",
                resultat.funnetAvklaringsbehov()
            )
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
            avklaringsbehovene.leggTil(resultat.funnetAvklaringsbehov(), aktivtSteg.type())
        }

        return resultat
    }

    private fun harAvklaringspunkt(
        steg: StegType,
        behandlingId: BehandlingId
    ): Transisjon {
        val relevanteAvklaringsbehov =
            avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId).alle()
                .filter { it.erÅpent() }
                .filter { behov -> behov.skalLøsesISteg(aktivtSteg.type()) }


        if (relevanteAvklaringsbehov.any { behov -> behov.skalStoppeHer(steg) }) {
            return Stopp
        }

        return Fortsett
    }

    private fun behandleStegBakover(kontekst: FlytKontekst): Transisjon {
        behandlingSteg.vedTilbakeføring(kontekst)
        return Fortsett
    }

    private fun loggStegHistorikk(
        behandling: Behandling,
        nyStegTilstand: StegTilstand
    ) {
        val førStatus = behandling.status()
        behandling.visit(nyStegTilstand)
        behandlingRepository.loggBesøktSteg(behandlingId = behandling.id, nyStegTilstand)
        val etterStatus = nyStegTilstand.steg().status
        if (førStatus != etterStatus) {
            behandlingRepository.oppdaterBehandlingStatus(behandlingId = behandling.id, status = etterStatus)
        }
    }
}
