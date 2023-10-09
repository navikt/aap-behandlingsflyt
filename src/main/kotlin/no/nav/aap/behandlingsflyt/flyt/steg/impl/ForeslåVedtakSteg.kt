package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class ForeslåVedtakSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val behandling = BehandlingTjeneste.hent(input.kontekst.behandlingId)

        if (harHattAvklaringsbehov(behandling) && harIkkeForeslåttVedtak(behandling)) {
            return StegResultat(listOf(Definisjon.FORESLÅ_VEDTAK))
        }

        return StegResultat() // DO NOTHING
    }

    private fun harHattAvklaringsbehov(behandling: Behandling) =
        behandling.avklaringsbehov().filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }.isNotEmpty()

    private fun harIkkeForeslåttVedtak(behandling: Behandling): Boolean {
        return behandling
            .avklaringsbehov()
            .filter { avklaringsbehov -> avklaringsbehov.erForeslåttVedtak() }
            .none { it.status() == Status.AVSLUTTET }
    }

    override fun type(): StegType {
        return StegType.FORESLÅ_VEDTAK
    }

    override fun vedTilbakeføring(input: StegInput) {
        val behandling = BehandlingTjeneste.hent(input.kontekst.behandlingId)
        val avklaringsbehovene = behandling.avklaringsbehovene()
        val relevanteBehov =
            avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FORESLÅ_VEDTAK))

        if (relevanteBehov.isNotEmpty()) {
            avklaringsbehovene.avbryt(Definisjon.FORESLÅ_VEDTAK)
        }
    }
}
