package no.nav.aap.flyt.kontroll

import no.nav.aap.domene.behandling.Behandling
import no.nav.aap.domene.behandling.BehandlingRepository
import no.nav.aap.domene.behandling.BehandlingType
import no.nav.aap.domene.behandling.Status
import no.nav.aap.domene.behandling.StegTilstand
import no.nav.aap.domene.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.domene.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.flyt.BehandlingFlyt
import no.nav.aap.flyt.Definisjon
import no.nav.aap.flyt.StegStatus
import no.nav.aap.flyt.StegType
import no.nav.aap.steg.BehandlingSteg
import no.nav.aap.steg.StartBehandlingSteg
import no.nav.aap.steg.StegInput
import no.nav.aap.steg.VurderYrkesskadeSteg

class FlytKontroller {

    var definisjoner = HashMap<BehandlingType, BehandlingFlyt>()
    var stegene = HashMap<StegType, BehandlingSteg>()

    init {
        definisjoner[BehandlingType.FØRSTEGANGSBEHANDLING] = Definisjon.førstegangsbehandling
        definisjoner[BehandlingType.REVURDERING] = Definisjon.revurdering

        // TODO: Må instansieres på en bedre måte
        Definisjon.førstegangsbehandling.stegene()
            .forEach { steg -> stegene[steg] = StartBehandlingSteg() }

        stegene[StegType.AVSLUTT_BEHANDLING] = StartBehandlingSteg()
        stegene[StegType.AVKLAR_YRKESSKADE] = VurderYrkesskadeSteg()
    }

    fun prosesserBehandling(kontekst: FlytKontekst) {
        val behandling = BehandlingRepository.hentBehandling(kontekst.behandlingId)

        validerTilstandBehandling(behandling, listOf())

        val behandlingFlyt = definisjoner.getValue(behandling.type)

        var aktivtSteg = behandling.aktivtSteg()
        var nesteStegStatus = aktivtSteg.tilstand.status()
        var nesteSteg = aktivtSteg.tilstand.steg()

        var kanFortsette = true
        while (kanFortsette) {
            val avklaringsbehov = behandling.avklaringsbehov()

            val result = utførTilstandsEndring(kontekst, nesteStegStatus, avklaringsbehov, nesteSteg, behandling)

            if (result.funnetAvklaringsbehov().isNotEmpty()) {
                validerPlassering(result.funnetAvklaringsbehov(), nesteSteg, nesteStegStatus)
                behandling.leggTil(result.funnetAvklaringsbehov())
            }

            kanFortsette = result.kanFortsette()

            if (kanFortsette) {
                aktivtSteg = behandling.aktivtSteg()
                nesteStegStatus = utledNesteStegStatus(aktivtSteg)
                nesteSteg = utledNesteSteg(aktivtSteg, nesteStegStatus, behandlingFlyt)
            }
        }
    }

    fun løsAvklaringsbehovOgFortsettProsessering(kontekst: FlytKontekst,
                                                 avklaringsbehov: List<AvklaringsbehovLøsning>) {
        val behandling = BehandlingRepository.hentBehandling(kontekst.behandlingId)

        validerTilstandBehandling(behandling, avklaringsbehov.map { it.definisjon })

        val behandlingFlyt = definisjoner.getValue(behandling.type)

        // løses det behov som fremtvinger tilbakehopp?
        if (skalHoppesTilbake(behandlingFlyt, behandling.aktivtSteg(), avklaringsbehov.map { it.definisjon })) {
            val tilSteg = utledSteg(behandlingFlyt, behandling.aktivtSteg(), avklaringsbehov.map { it.definisjon })
            val tilStegStatus = utledStegStatus(avklaringsbehov.filter { it.definisjon.løsesISteg == tilSteg }
                .map { it.definisjon.vurderingspunkt.stegStatus })

            hoppTilbakeTilSteg(kontekst, behandling, tilSteg, tilStegStatus)
        } else if (skalRekjøreSteg(avklaringsbehov, behandling)) {
            flyttTilStartAvAktivtSteg(behandling)
        }

        avklaringsbehov.forEach { behandling.løsAvklaringsbehov(it.definisjon, it.begrunnelse, it.endretAv) }


        prosesserBehandling(kontekst)
    }

    private fun validerTilstandBehandling(behandling: Behandling,
                                          avklaringsbehov: List<no.nav.aap.domene.behandling.avklaringsbehov.Definisjon>) {
        if (setOf(Status.AVSLUTTET, Status.IVERKSETTES).contains(behandling.status())) {
            throw IllegalArgumentException("Forsøker manipulere på behandling som er avsluttet")
        }
        if (avklaringsbehov.any { !behandling.avklaringsbehov().map { a -> a.definisjon }.contains(it) }) {
            throw IllegalArgumentException("Forsøker løse aksjonspunkt ikke knyttet til behandlingen")
        }
    }

    private fun hoppTilbakeTilSteg(kontekst: FlytKontekst,
                                   behandling: Behandling,
                                   tilSteg: StegType,
                                   tilStegStatus: StegStatus) {
        val behandlingFlyt = definisjoner.getValue(behandling.type)

        val aktivtSteg = behandling.aktivtSteg()
        var forrige = aktivtSteg.tilstand.steg()

        var kanFortsette = true
        while (kanFortsette) {
            forrige = behandlingFlyt.forrige(forrige)

            if (forrige == tilSteg && tilStegStatus != StegStatus.INNGANG) {
                kanFortsette = false
            } else {
                utførTilstandsEndring(
                    kontekst = kontekst,
                    nesteStegStatus = StegStatus.TILBAKEFØRT,
                    avklaringsbehov = listOf(),
                    aktivtSteg = forrige,
                    behandling = behandling
                )
            }
            if (forrige == tilSteg && tilStegStatus == StegStatus.INNGANG) {
                kanFortsette = false
            }
        }
    }

    private fun flyttTilStartAvAktivtSteg(behandling: Behandling) {
        val nyStegTilstand =
            StegTilstand(tilstand = no.nav.aap.flyt.Tilstand(behandling.aktivtSteg().tilstand.steg(), StegStatus.START))
        behandling.visit(nyStegTilstand)
    }

    private fun utledStegStatus(stegStatuser: List<StegStatus>): StegStatus {
        if (stegStatuser.contains(StegStatus.UTGANG)) {
            return StegStatus.UTGANG
        }
        return StegStatus.INNGANG
    }

    private fun utledSteg(behandlingFlyt: BehandlingFlyt,
                          aktivtSteg: StegTilstand,
                          map: List<no.nav.aap.domene.behandling.avklaringsbehov.Definisjon>): StegType {
        TODO("Not yet implemented")
    }

    private fun skalRekjøreSteg(avklaringsbehov: List<AvklaringsbehovLøsning>,
                                behandling: Behandling) =
        avklaringsbehov.filter { it.definisjon.løsesISteg == behandling.aktivtSteg().tilstand.steg() }
            .filter { it.definisjon.rekjørSteg }.isNotEmpty()

    private fun skalHoppesTilbake(behandlingFlyt: BehandlingFlyt,
                                  aktivtSteg: StegTilstand,
                                  avklaringsDefinisjoner: List<no.nav.aap.domene.behandling.avklaringsbehov.Definisjon>): Boolean {

        return avklaringsDefinisjoner.filter { definisjon ->
            behandlingFlyt.erStegFør(
                aktivtSteg.tilstand.steg(),
                definisjon.løsesISteg
            )
        }.isNotEmpty()
    }

    private fun validerPlassering(funnetAvklaringsbehov: List<no.nav.aap.domene.behandling.avklaringsbehov.Definisjon>,
                                  nesteSteg: StegType,
                                  nesteStegStatus: StegStatus) {
        // TODO("Not yet implemented")
    }

    private fun utledNesteSteg(aktivtSteg: StegTilstand,
                               nesteStegStatus: StegStatus,
                               behandlingFlyt: BehandlingFlyt): StegType {

        if (aktivtSteg.tilstand.status() == StegStatus.AVSLUTTER && nesteStegStatus == StegStatus.START) {
            return behandlingFlyt.neste(aktivtSteg.tilstand.steg())
        }
        return aktivtSteg.tilstand.steg()
    }

    private fun utførTilstandsEndring(kontekst: FlytKontekst,
                                      nesteStegStatus: StegStatus,
                                      avklaringsbehov: List<Avklaringsbehov>,
                                      aktivtSteg: StegType,
                                      behandling: Behandling): Transisjon {
        val relevanteAvklaringsbehov = avklaringsbehov.filter { behov -> behov.definisjon.skalLøsesISteg(aktivtSteg) }
        return when (nesteStegStatus) {
            StegStatus.INNGANG -> harAvklaringspunkt(aktivtSteg, nesteStegStatus, relevanteAvklaringsbehov)
            StegStatus.UTFØRER -> behandleSteg(aktivtSteg, kontekst)
            StegStatus.UTGANG -> harAvklaringspunkt(aktivtSteg, nesteStegStatus, relevanteAvklaringsbehov)
            StegStatus.AVSLUTTER -> harTruffetSlutten(aktivtSteg)
            StegStatus.TILBAKEFØRT -> behandleStegBakover(aktivtSteg, kontekst)
            else -> Fortsett
        }.also {
            val nyStegTilstand =
                StegTilstand(tilstand = no.nav.aap.flyt.Tilstand(aktivtSteg, nesteStegStatus))
            behandling.visit(nyStegTilstand)
        }
    }

    private fun behandleStegBakover(steg: StegType, kontekst: FlytKontekst): Transisjon {
        val input = StegInput(kontekst)
        stegene.getValue(steg).vedTilbakeføring(input)

        return Fortsett
    }

    private fun harTruffetSlutten(aktivtSteg: StegType): Transisjon {
        return when (aktivtSteg) {
            StegType.AVSLUTT_BEHANDLING -> Stopp
            else -> Fortsett
        }
    }

    private fun behandleSteg(steg: StegType, kontekst: FlytKontekst): Transisjon {
        val input = StegInput(kontekst)
        val stegResultat = stegene.getValue(steg).utfør(input)

        return stegResultat.transisjon()
    }

    private fun harAvklaringspunkt(steg: StegType,
                                   nesteStegStatus: StegStatus,
                                   avklaringsbehov: List<Avklaringsbehov>): Transisjon {

        if (avklaringsbehov.any { behov -> behov.skalStoppeHer(steg, nesteStegStatus) }) {
            return Stopp
        }

        return Fortsett
    }

    private fun utledNesteStegStatus(aktivtSteg: StegTilstand): StegStatus {
        val status = aktivtSteg.tilstand.status()

        return StegStatus.neste(status)
    }
}
