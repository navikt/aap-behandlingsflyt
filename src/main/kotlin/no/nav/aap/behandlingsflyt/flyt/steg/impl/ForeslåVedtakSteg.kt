package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat

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
            .filter { avklaringsbehov -> avklaringsbehov.definisjon == Definisjon.FORESLÅ_VEDTAK }
            .none { it.status() == Status.AVSLUTTET }
    }

    override fun type(): StegType {
        return StegType.FORESLÅ_VEDTAK
    }
}
