package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.domene.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(StegOrkestrator::class.java)

class StegOrkestrator(private val aktivtSteg: BehandlingSteg) {

    fun utfør(
        kontekst: FlytKontekst,
        avklaringsbehov: List<Avklaringsbehov>,
        behandling: Behandling
    ): Transisjon {

        var gjeldendeStegStatus = StegStatus.START
        while (true) {
            val resultat = utførTilstandsEndring(kontekst, gjeldendeStegStatus, avklaringsbehov, behandling)

            if (resultat.funnetAvklaringsbehov().isNotEmpty()) {
                log.info(
                    "[{} - {}] Fant avklaringsbehov: {}",
                    kontekst.sakId,
                    kontekst.behandlingId,
                    resultat.funnetAvklaringsbehov()
                )
                behandling.leggTil(resultat.funnetAvklaringsbehov())
            }

            if (!resultat.kanFortsette() || resultat.erTilbakeføring() || gjeldendeStegStatus == StegStatus.AVSLUTTER) {
                return resultat
            }
            gjeldendeStegStatus = gjeldendeStegStatus.neste()
        }
    }

    fun utførTilbakefør(
        kontekst: FlytKontekst,
        avklaringsbehov: List<Avklaringsbehov>,
        behandling: Behandling
    ): Transisjon {
        return utførTilstandsEndring(kontekst, StegStatus.TILBAKEFØRT, avklaringsbehov, behandling)
    }

    private fun utførTilstandsEndring(
        kontekst: FlytKontekst,
        nesteStegStatus: StegStatus,
        avklaringsbehov: List<Avklaringsbehov>,
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
            StegStatus.UTFØRER -> behandleSteg(aktivtSteg, kontekst)
            StegStatus.AVKLARINGSPUNKT -> harAvklaringspunkt(aktivtSteg.type(), relevanteAvklaringsbehov)
            StegStatus.AVSLUTTER -> Fortsett
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

    private fun behandleSteg(steg: BehandlingSteg, kontekst: FlytKontekst): Transisjon {
        val input = StegInput(kontekst)
        val stegResultat = steg.utfør(input)

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