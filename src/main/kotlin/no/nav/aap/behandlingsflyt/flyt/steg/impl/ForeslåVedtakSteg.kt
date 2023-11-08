package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat

class ForeslåVedtakSteg(private val behandlingService: BehandlingService) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val behandling = behandlingService.hent(kontekst.behandlingId)

        if (behandling.harHattAvklaringsbehov() && behandling.harIkkeForeslåttVedtak()) {
            return StegResultat(listOf(Definisjon.FORESLÅ_VEDTAK))
        }

        return StegResultat() // DO NOTHING
    }

    override fun vedTilbakeføring(kontekst: FlytKontekst) {
        val behandling = behandlingService.hent(kontekst.behandlingId)
        val avklaringsbehovene = behandling.avklaringsbehovene()
        val relevanteBehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FORESLÅ_VEDTAK))

        if (relevanteBehov.isNotEmpty()) {
            avklaringsbehovene.avbryt(Definisjon.FORESLÅ_VEDTAK)
        }
    }
}
