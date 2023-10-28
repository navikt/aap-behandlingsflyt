package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(StegOrkestrator::class.java)

class StegOrkestrator(private val transaksjonsconnection: DBConnection, private val aktivtSteg: FlytSteg) {

    fun utfør(
        kontekst: FlytKontekst,
        behandling: Behandling
    ): Transisjon {

        var gjeldendeStegStatus = StegStatus.START
        while (true) {
            val resultat = utførTilstandsEndring(kontekst, gjeldendeStegStatus, behandling)

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
            behandling.avklaringsbehov()
                .filter { it.erÅpent() }
                .filter { behov -> behov.skalLøsesISteg(aktivtSteg.type()) }

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

    private fun behandleStegBakover(flytSteg: FlytSteg, kontekst: FlytKontekst): Transisjon {
        val input = StegInput(kontekst)
        val steg = flytSteg.konstruer(transaksjonsconnection)
        steg.vedTilbakeføring(input)

        return Fortsett
    }

    private fun behandleSteg(flytSteg: FlytSteg, kontekst: FlytKontekst): Transisjon {
        val input = StegInput(kontekst)
        val steg = flytSteg.konstruer(transaksjonsconnection)
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
