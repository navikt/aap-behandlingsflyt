package no.nav.aap.flyt.kontroll

import no.nav.aap.avklaringsbehov.AvklaringsbehovLøsning
import no.nav.aap.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.avklaringsbehov.sykdom.AvklarSykdomLøser
import no.nav.aap.avklaringsbehov.vedtak.FatteVedtakLøser
import no.nav.aap.avklaringsbehov.vedtak.ForeslåVedtakLøser
import no.nav.aap.domene.behandling.Behandling
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.StegTilstand
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.BehandlingFlyt

class AvklaringsbehovKontroller {

    private val avklaringsbehovsLøsere = mutableMapOf<Definisjon, AvklaringsbehovsLøser<*>>()
    private val flytKontroller = FlytKontroller()

    init {
        avklaringsbehovsLøsere[Definisjon.AVKLAR_SYKDOM] = AvklarSykdomLøser()
        avklaringsbehovsLøsere[Definisjon.FORESLÅ_VEDTAK] = ForeslåVedtakLøser()
        avklaringsbehovsLøsere[Definisjon.FATTE_VEDTAK] = FatteVedtakLøser()
    }

    fun løsAvklaringsbehovOgFortsettProsessering(
        kontekst: FlytKontekst,
        avklaringsbehov: List<AvklaringsbehovLøsning>
    ) {
        val behandling = BehandlingTjeneste.hent(kontekst.behandlingId)

        ValiderBehandlingTilstand.validerTilstandBehandling(behandling, avklaringsbehov.map { it.definisjon() })

        val behandlingFlyt = behandling.type.flyt()

        // løses det behov som fremtvinger tilbakehopp?
        if (skalHoppesTilbake(behandlingFlyt, behandling.aktivtSteg(), avklaringsbehov.map { it.definisjon() })) {
            val tilSteg = flytKontroller.utledSteg(
                behandlingFlyt,
                behandling.aktivtSteg(),
                avklaringsbehov.map { it.definisjon() })
            val tilStegStatus =
                flytKontroller.utledStegStatus(avklaringsbehov.filter { it.definisjon().løsesISteg == tilSteg }
                    .map { it.definisjon().vurderingspunkt.stegStatus })

            flytKontroller.hoppTilbakeTilSteg(kontekst, behandling, tilSteg, tilStegStatus)
        } else if (skalRekjøreSteg(avklaringsbehov, behandling)) {
            flytKontroller.flyttTilStartAvAktivtSteg(behandling)
        }

        // Bør ideelt kalle på
        avklaringsbehov.forEach { løsAvklaringsbehov(kontekst, behandling, it) }

        flytKontroller.prosesserBehandling(kontekst)
    }

    @Suppress("UNCHECKED_CAST")
    private fun løsAvklaringsbehov(
        kontekst: FlytKontekst,
        behandling: Behandling,
        it: AvklaringsbehovLøsning
    ) {
        // Liker denne casten fryktelig lite godt -_- men må til pga generics *
        val avklaringsbehovsLøser =
            avklaringsbehovsLøsere.getValue(it.definisjon()) as AvklaringsbehovsLøser<AvklaringsbehovLøsning>
        avklaringsbehovsLøser.løs(kontekst = kontekst, it)
        behandling.løsAvklaringsbehov(it.definisjon(), it.begrunnelse, it.endretAv)
    }


    private fun skalRekjøreSteg(
        avklaringsbehov: List<AvklaringsbehovLøsning>,
        behandling: Behandling
    ) =
        avklaringsbehov.filter { it.definisjon().løsesISteg == behandling.aktivtSteg().tilstand.steg() }
            .any { it.definisjon().rekjørSteg }

    private fun skalHoppesTilbake(
        behandlingFlyt: BehandlingFlyt,
        aktivtSteg: StegTilstand,
        avklaringsDefinisjoner: List<Definisjon>
    ): Boolean {

        return avklaringsDefinisjoner.filter { definisjon ->
            behandlingFlyt.erStegFør(
                definisjon.løsesISteg,
                aktivtSteg.tilstand.steg()
            )
        }.isNotEmpty()
    }
}
