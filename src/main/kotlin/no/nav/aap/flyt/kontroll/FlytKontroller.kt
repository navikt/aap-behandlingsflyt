package no.nav.aap.flyt.kontroll

import no.nav.aap.avklaringsbehov.AvklaringsbehovLøsning
import no.nav.aap.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.avklaringsbehov.yrkesskade.AvklarYrkesskadeLøser
import no.nav.aap.domene.behandling.Behandling
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.Status
import no.nav.aap.domene.behandling.StegTilstand
import no.nav.aap.domene.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.BehandlingFlyt
import no.nav.aap.flyt.StegStatus
import no.nav.aap.flyt.StegType
import no.nav.aap.flyt.steg.BehandlingSteg
import no.nav.aap.flyt.steg.StegInput

class FlytKontroller {

    private val avklaringsbehovsLøsere = mutableMapOf<Definisjon, AvklaringsbehovsLøser<*>>()

    init {
        avklaringsbehovsLøsere[Definisjon.AVKLAR_YRKESSKADE] = AvklarYrkesskadeLøser()
    }

    fun prosesserBehandling(kontekst: FlytKontekst) {
        val behandling = BehandlingTjeneste.hentBehandling(kontekst.behandlingId)

        validerTilstandBehandling(behandling, listOf())

        val behandlingFlyt = behandling.type.flyt()

        var aktivtSteg = behandling.aktivtSteg()
        var nesteStegStatus = aktivtSteg.tilstand.status()
        var nesteSteg = behandlingFlyt.steg(aktivtSteg.tilstand.steg())

        var kanFortsette = true
        while (kanFortsette) {
            val avklaringsbehov = behandling.avklaringsbehov().filter { behov -> behov.erÅpent() }
            validerPlassering(
                behandlingFlyt,
                avklaringsbehov.map { behov -> behov.definisjon },
                nesteSteg.type(),
                nesteStegStatus
            )

            val result = utførTilstandsEndring(kontekst, nesteStegStatus, avklaringsbehov, nesteSteg, behandling)

            if (result.funnetAvklaringsbehov().isNotEmpty()) {
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
        val behandling = BehandlingTjeneste.hentBehandling(kontekst.behandlingId)

        validerTilstandBehandling(behandling, avklaringsbehov.map { it.definisjon })

        val behandlingFlyt = behandling.type.flyt()

        // løses det behov som fremtvinger tilbakehopp?
        if (skalHoppesTilbake(behandlingFlyt, behandling.aktivtSteg(), avklaringsbehov.map { it.definisjon })) {
            val tilSteg = utledSteg(behandlingFlyt, behandling.aktivtSteg(), avklaringsbehov.map { it.definisjon })
            val tilStegStatus = utledStegStatus(avklaringsbehov.filter { it.definisjon.løsesISteg == tilSteg }
                .map { it.definisjon.vurderingspunkt.stegStatus })

            hoppTilbakeTilSteg(kontekst, behandling, tilSteg, tilStegStatus)
        } else if (skalRekjøreSteg(avklaringsbehov, behandling)) {
            flyttTilStartAvAktivtSteg(behandling)
        }

        // Bør ideelt kalle på
        avklaringsbehov.forEach { løsAvklaringsbehov(kontekst, behandling, it) }

        prosesserBehandling(kontekst)
    }

    @Suppress("UNCHECKED_CAST")
    private fun løsAvklaringsbehov(kontekst: FlytKontekst,
                                   behandling: Behandling,
                                   it: AvklaringsbehovLøsning) {
        // Liker denne casten fryktelig lite godt -_- men må til pga generics *
        val avklaringsbehovsLøser =
            avklaringsbehovsLøsere.getValue(it.definisjon) as AvklaringsbehovsLøser<AvklaringsbehovLøsning>
        avklaringsbehovsLøser.løs(kontekst = kontekst, it)
        behandling.løsAvklaringsbehov(it.definisjon, it.begrunnelse, it.endretAv)
    }

    private fun validerTilstandBehandling(behandling: Behandling,
                                          avklaringsbehov: List<Definisjon>) {
        if (setOf(Status.AVSLUTTET, Status.IVERKSETTES).contains(behandling.status())) {
            throw IllegalArgumentException("Forsøker manipulere på behandling som er avsluttet")
        }
        if (avklaringsbehov.any { !behandling.avklaringsbehov().map { a -> a.definisjon }.contains(it) }) {
            throw IllegalArgumentException("Forsøker løse aksjonspunkt ikke knyttet til behandlingen")
        }
        if (avklaringsbehov.any {
                !behandling.type.flyt().erStegFørEllerLik(it.løsesISteg, behandling.aktivtSteg().tilstand.steg())
            }) {
            throw IllegalArgumentException("Forsøker løse aksjonspunkt ikke knyttet til behandlingen")
        }
    }

    private fun hoppTilbakeTilSteg(kontekst: FlytKontekst,
                                   behandling: Behandling,
                                   tilSteg: StegType,
                                   tilStegStatus: StegStatus) {
        val behandlingFlyt = behandling.type.flyt()

        val aktivtSteg = behandling.aktivtSteg()
        var forrige = behandlingFlyt.steg(aktivtSteg.tilstand.steg())

        var kanFortsette = true
        while (kanFortsette) {
            forrige = behandlingFlyt.forrige(forrige.type())

            if (forrige.type() == tilSteg && tilStegStatus != StegStatus.INNGANG) {
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
            if (forrige.type() == tilSteg && tilStegStatus == StegStatus.INNGANG) {
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
                          avklaringsDefinisjoner: List<Definisjon>): StegType {
        return avklaringsDefinisjoner.filter { definisjon ->
            behandlingFlyt.erStegFør(
                aktivtSteg.tilstand.steg(),
                definisjon.løsesISteg
            )
        }
            .map { definisjon -> definisjon.løsesISteg }
            .minWith(behandlingFlyt.compareable())
    }

    private fun skalRekjøreSteg(avklaringsbehov: List<AvklaringsbehovLøsning>,
                                behandling: Behandling) =
        avklaringsbehov.filter { it.definisjon.løsesISteg == behandling.aktivtSteg().tilstand.steg() }
            .any { it.definisjon.rekjørSteg }

    private fun skalHoppesTilbake(behandlingFlyt: BehandlingFlyt,
                                  aktivtSteg: StegTilstand,
                                  avklaringsDefinisjoner: List<Definisjon>): Boolean {

        return avklaringsDefinisjoner.filter { definisjon ->
            behandlingFlyt.erStegFør(
                aktivtSteg.tilstand.steg(),
                definisjon.løsesISteg
            )
        }.isNotEmpty()
    }

    private fun validerPlassering(behandlingFlyt: BehandlingFlyt,
                                  åpneAvklaringsbehov: List<Definisjon>,
                                  nesteSteg: StegType,
                                  nesteStegStatus: StegStatus) {
        val uhåndterteBehov = åpneAvklaringsbehov
            .filter { definisjon ->
                behandlingFlyt.erStegFørEllerLik(
                    definisjon.løsesISteg,
                    nesteSteg
                )
            }
            .filter { definisjon ->
                behandlingFlyt.erStegFør(
                    definisjon.løsesISteg,
                    nesteSteg
                ) || definisjon.vurderingspunkt.stegStatus.erFør(nesteStegStatus)
            }
        if (uhåndterteBehov.isNotEmpty()) {
            throw IllegalStateException("Har uhåndterte behov som skulle vært håndtert før nåværende steg = '$nesteSteg' med status = '$nesteStegStatus'")
        }
    }

    private fun utledNesteSteg(aktivtSteg: StegTilstand,
                               nesteStegStatus: StegStatus,
                               behandlingFlyt: BehandlingFlyt): BehandlingSteg {

        if (aktivtSteg.tilstand.status() == StegStatus.AVSLUTTER && nesteStegStatus == StegStatus.START) {
            return behandlingFlyt.neste(aktivtSteg.tilstand.steg())
        }
        return behandlingFlyt.steg(aktivtSteg.tilstand.steg())
    }

    private fun utførTilstandsEndring(kontekst: FlytKontekst,
                                      nesteStegStatus: StegStatus,
                                      avklaringsbehov: List<Avklaringsbehov>,
                                      aktivtSteg: BehandlingSteg,
                                      behandling: Behandling): Transisjon {
        val relevanteAvklaringsbehov =
            avklaringsbehov.filter { behov -> behov.definisjon.skalLøsesISteg(aktivtSteg.type()) }
        return when (nesteStegStatus) {
            StegStatus.INNGANG -> harAvklaringspunkt(aktivtSteg.type(), nesteStegStatus, relevanteAvklaringsbehov)
            StegStatus.UTFØRER -> behandleSteg(aktivtSteg, kontekst)
            StegStatus.UTGANG -> harAvklaringspunkt(aktivtSteg.type(), nesteStegStatus, relevanteAvklaringsbehov)
            StegStatus.AVSLUTTER -> harTruffetSlutten(aktivtSteg.type())
            StegStatus.TILBAKEFØRT -> behandleStegBakover(aktivtSteg, kontekst)
            else -> Fortsett
        }.also {
            val nyStegTilstand =
                StegTilstand(tilstand = no.nav.aap.flyt.Tilstand(aktivtSteg.type(), nesteStegStatus))
            behandling.visit(nyStegTilstand)
        }
    }

    private fun behandleStegBakover(steg: BehandlingSteg, kontekst: FlytKontekst): Transisjon {
        val input = StegInput(kontekst)
        steg.vedTilbakeføring(input)

        return Fortsett
    }

    private fun harTruffetSlutten(aktivtSteg: StegType): Transisjon {
        return when (aktivtSteg) {
            StegType.AVSLUTT_BEHANDLING -> Stopp
            else -> Fortsett
        }
    }

    private fun behandleSteg(steg: BehandlingSteg, kontekst: FlytKontekst): Transisjon {
        val input = StegInput(kontekst)
        val stegResultat = steg.utfør(input)

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
