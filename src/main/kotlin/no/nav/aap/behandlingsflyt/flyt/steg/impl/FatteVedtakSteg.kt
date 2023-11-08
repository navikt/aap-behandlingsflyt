package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat

class FatteVedtakSteg(private val behandlingService: BehandlingService) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val behandling = behandlingService.hent(kontekst.behandlingId)

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
}
