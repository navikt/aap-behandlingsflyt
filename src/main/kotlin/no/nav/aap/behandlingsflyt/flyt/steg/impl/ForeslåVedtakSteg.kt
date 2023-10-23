package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class ForeslåVedtakSteg(private val behandlingTjeneste: BehandlingTjeneste) : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val behandling = behandlingTjeneste.hent(input.kontekst.behandlingId)

        if (behandling.harHattAvklaringsbehov() && behandling.harIkkeForeslåttVedtak()) {
            return StegResultat(listOf(Definisjon.FORESLÅ_VEDTAK))
        }

        return StegResultat() // DO NOTHING
    }

    override fun type(): StegType {
        return StegType.FORESLÅ_VEDTAK
    }

    override fun vedTilbakeføring(input: StegInput) {
        val behandling = behandlingTjeneste.hent(input.kontekst.behandlingId)
        val avklaringsbehovene = behandling.avklaringsbehovene()
        val relevanteBehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FORESLÅ_VEDTAK))

        if (relevanteBehov.isNotEmpty()) {
            avklaringsbehovene.avbryt(Definisjon.FORESLÅ_VEDTAK)
        }
    }
}
