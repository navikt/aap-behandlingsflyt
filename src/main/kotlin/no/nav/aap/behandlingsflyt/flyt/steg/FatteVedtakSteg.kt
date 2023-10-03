package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.domene.behandling.Behandling
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.flyt.StegType

class FatteVedtakSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val behandling = BehandlingTjeneste.hent(input.kontekst.behandlingId)

        if (skalTilbakeføresEtterTotrinnsVurdering(behandling)) {
            val tilbakeførtFraBeslutter = behandling.avklaringsbehovene().tilbakeførtFraBeslutter()
            val førsteSteg = tilbakeførtFraBeslutter.map { it.løsesISteg() }
                .minWith(behandling.flyt().compareable())

            return StegResultat(tilbakeførtTilSteg = førsteSteg)
        }
        if (harHattAvklaringsbehovSomHarKrevdToTrinn(behandling)) {
            return StegResultat(listOf(Definisjon.FATTE_VEDTAK))
        }

        return StegResultat() // DO NOTHING
    }

    private fun skalTilbakeføresEtterTotrinnsVurdering(behandling: Behandling): Boolean {
        return behandling.avklaringsbehov()
            .any { avklaringsbehov -> avklaringsbehov.status() == Status.SENDT_TILBAKE_FRA_BESLUTTER }
    }

    private fun harHattAvklaringsbehovSomHarKrevdToTrinn(behandling: Behandling) =
        behandling.avklaringsbehov()
            .filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
            .any { avklaringsbehov -> avklaringsbehov.erTotrinn() && !avklaringsbehov.erTotrinnsVurdert() }

    override fun type(): StegType {
        return StegType.FATTE_VEDTAK
    }
}
