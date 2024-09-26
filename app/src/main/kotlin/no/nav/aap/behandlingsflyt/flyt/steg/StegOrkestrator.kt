package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.InformasjonskravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.periodisering.PerioderTilVurderingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingFlytRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegStatus
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
class StegOrkestrator(
    private val connection: DBConnection,
    private val aktivtSteg: FlytSteg
) {

    private val behandlingRepository = BehandlingFlytRepository(connection)
    private val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)
    private val perioderTilVurderingService = PerioderTilVurderingService(connection)
    private val informasjonskravGrunnlag = InformasjonskravGrunnlag(connection)

    private val behandlingSteg = aktivtSteg.konstruer(connection)

    fun utfør(
        kontekst: FlytKontekst,
        behandling: Behandling,
        faktagrunnlagForGjeldendeSteg: List<Informasjonskravkonstruktør>
    ): Transisjon {
        var gjeldendeStegStatus = StegStatus.START
        log.info("Behandler steg '{}'", aktivtSteg.type())

        val kontekstMedPerioder = FlytKontekstMedPerioder(
            sakId = kontekst.sakId,
            behandlingId = kontekst.behandlingId,
            behandlingType = kontekst.behandlingType,
            perioderTilVurdering = perioderTilVurderingService.utled(
                kontekst = kontekst,
                stegType = aktivtSteg.type()
            )
        )

        while (true) {
            val resultat = utførTilstandsEndring(
                kontekstMedPerioder,
                gjeldendeStegStatus,
                behandling,
                faktagrunnlagForGjeldendeSteg
            )
            if (gjeldendeStegStatus in setOf(StegStatus.START, StegStatus.OPPDATER_FAKTAGRUNNLAG)) {
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
        val kontekstMedPerioder = FlytKontekstMedPerioder(
            sakId = kontekst.sakId,
            behandlingId = kontekst.behandlingId,
            behandlingType = kontekst.behandlingType,
            perioderTilVurdering = perioderTilVurderingService.utled(
                kontekst = kontekst,
                stegType = aktivtSteg.type()
            )
        )
        return utførTilstandsEndring(
            kontekstMedPerioder,
            StegStatus.TILBAKEFØRT,
            behandling,
            listOf()
        )
    }

    private fun utførTilstandsEndring(
        kontekst: FlytKontekstMedPerioder,
        nesteStegStatus: StegStatus,
        behandling: Behandling,
        faktagrunnlagForGjeldendeSteg: List<Informasjonskravkonstruktør>
    ): Transisjon {
        log.debug(
            "Behandler steg({}) med status({})",
            aktivtSteg.type(),
            nesteStegStatus
        )
        val transisjon = when (nesteStegStatus) {
            StegStatus.UTFØRER -> behandleSteg(kontekst)
            StegStatus.OPPDATER_FAKTAGRUNNLAG -> oppdaterFaktagrunnlag(kontekst, faktagrunnlagForGjeldendeSteg)
            StegStatus.AVKLARINGSPUNKT -> harAvklaringspunkt(aktivtSteg.type(), kontekst.behandlingId)
            StegStatus.AVSLUTTER -> Fortsett
            StegStatus.TILBAKEFØRT -> behandleStegBakover(kontekst)
            else -> Fortsett
        }

        val nyStegTilstand = StegTilstand(stegType = aktivtSteg.type(), stegStatus = nesteStegStatus)
        loggStegHistorikk(behandling, nyStegTilstand)

        return transisjon
    }

    private fun oppdaterFaktagrunnlag(
        kontekstMedPerioder: FlytKontekstMedPerioder,
        faktagrunnlagForGjeldendeSteg: List<Informasjonskravkonstruktør>
    ): Fortsett {
        informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(
            faktagrunnlagForGjeldendeSteg,
            kontekstMedPerioder
        )
        return Fortsett
    }

    private fun behandleSteg(kontekstMedPerioder: FlytKontekstMedPerioder): Transisjon {
        val stegResultat = behandlingSteg.utfør(kontekstMedPerioder)

        val resultat = stegResultat.transisjon()

        if (resultat.funnetAvklaringsbehov().isNotEmpty()) {
            log.info(
                "Fant avklaringsbehov: {}",
                resultat.funnetAvklaringsbehov()
            )
            val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekstMedPerioder.behandlingId)
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

    private fun behandleStegBakover(kontekst: FlytKontekstMedPerioder): Transisjon {
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
