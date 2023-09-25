package no.nav.aap.flyt.kontroll

import no.nav.aap.avklaringsbehov.AvklaringsbehovLøsning
import no.nav.aap.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.avklaringsbehov.SattPåVentLøser
import no.nav.aap.avklaringsbehov.sykdom.AvklarSykdomLøser
import no.nav.aap.avklaringsbehov.vedtak.FatteVedtakLøser
import no.nav.aap.avklaringsbehov.vedtak.ForeslåVedtakLøser
import no.nav.aap.domene.behandling.Behandling
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.StegTilstand
import no.nav.aap.domene.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.flyt.BehandlingFlyt
import org.slf4j.LoggerFactory

class AvklaringsbehovKontroller {

    private val avklaringsbehovsLøsere = mutableMapOf<Definisjon, AvklaringsbehovsLøser<*>>()
    private val flytKontroller = FlytKontroller()

    private val log = LoggerFactory.getLogger(AvklaringsbehovKontroller::class.java)

    init {
        avklaringsbehovsLøsere[Definisjon.MANUELT_SATT_PÅ_VENT] = SattPåVentLøser()
        avklaringsbehovsLøsere[Definisjon.AVKLAR_SYKDOM] = AvklarSykdomLøser()
        avklaringsbehovsLøsere[Definisjon.FORESLÅ_VEDTAK] = ForeslåVedtakLøser()
        avklaringsbehovsLøsere[Definisjon.FATTE_VEDTAK] = FatteVedtakLøser()
    }

    fun løsAvklaringsbehovOgFortsettProsessering(
        kontekst: FlytKontekst,
        avklaringsbehov: List<AvklaringsbehovLøsning>
    ) {
        løsAvklaringsbehov(kontekst, avklaringsbehov)

        flytKontroller.prosesserBehandling(kontekst)
    }

    fun løsAvklaringsbehov(
        kontekst: FlytKontekst,
        avklaringsbehov: List<AvklaringsbehovLøsning>
    ) {
        val behandling = BehandlingTjeneste.hent(kontekst.behandlingId)
        log.info("Forsøker løse avklaringsbehov på behandling[${behandling.referanse}")

        val definisjoner = avklaringsbehov.map { løsning -> løsning.definisjon() }

        ValiderBehandlingTilstand.validerTilstandBehandling(behandling, definisjoner)

        // løses det behov som fremtvinger tilbakehopp?
        flytKontroller.forberedLøsingAvBehov(definisjoner, behandling, kontekst)

        // Bør ideelt kalle på
        avklaringsbehov.forEach { løsAvklaringsbehov(kontekst, behandling, it) }
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
        val løsningsResultat = avklaringsbehovsLøser.løs(kontekst = kontekst, løsning = it)
        behandling.løsAvklaringsbehov(
            it.definisjon(),
            løsningsResultat.begrunnelse,
            "Saksbehandler"
        ) // TODO: Hente fra context
    }
}
