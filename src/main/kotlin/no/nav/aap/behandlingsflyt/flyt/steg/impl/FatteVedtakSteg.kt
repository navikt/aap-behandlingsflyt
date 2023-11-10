package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat

class FatteVedtakSteg(
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val avklaringsbehov = avklaringsbehovRepository.hent(kontekst.behandlingId)

        if (skalTilbakeføresEtterTotrinnsVurdering(avklaringsbehov)) {
            return StegResultat(tilbakeførtFraBeslutter = true)
        }
        if (harHattAvklaringsbehovSomHarKrevdToTrinn(avklaringsbehov)) {
            return StegResultat(listOf(Definisjon.FATTE_VEDTAK))
        }

        return StegResultat()
    }

    private fun skalTilbakeføresEtterTotrinnsVurdering(avklaringsbehovene: Avklaringsbehovene): Boolean {
        return avklaringsbehovene.tilbakeførtFraBeslutter().isNotEmpty()
    }

    private fun harHattAvklaringsbehovSomHarKrevdToTrinn(avklaringsbehovene: Avklaringsbehovene) =
        avklaringsbehovene.alle()
            .filter { avklaringsbehov -> avklaringsbehov.erIkkeAvbrutt() }
            .any { avklaringsbehov -> avklaringsbehov.erTotrinn() && !avklaringsbehov.erTotrinnsVurdert() }
}
