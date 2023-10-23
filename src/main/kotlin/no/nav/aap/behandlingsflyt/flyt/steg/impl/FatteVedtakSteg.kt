package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class FatteVedtakSteg(private val behandlingTjeneste: BehandlingTjeneste) : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val behandling = behandlingTjeneste.hent(input.kontekst.behandlingId)

        if (skalTilbakeføresEtterTotrinnsVurdering(behandling)) {
            return StegResultat(tilbakeførtFraBeslutter = true)
        }
        if (harHattAvklaringsbehovSomHarKrevdToTrinn(behandling)) {
            return StegResultat(listOf(Definisjon.FATTE_VEDTAK))
        }

        return StegResultat()
    }

    private fun skalTilbakeføresEtterTotrinnsVurdering(behandling: Behandling): Boolean {
        return behandling.avklaringsbehovene().tilbakeførtFraBeslutter().isNotEmpty()
    }

    private fun harHattAvklaringsbehovSomHarKrevdToTrinn(behandling: Behandling) =
        behandling.avklaringsbehov()
            .filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
            .any { avklaringsbehov -> avklaringsbehov.erTotrinn() && !avklaringsbehov.erTotrinnsVurdert() }

    override fun type(): StegType {
        return StegType.FATTE_VEDTAK
    }
}
