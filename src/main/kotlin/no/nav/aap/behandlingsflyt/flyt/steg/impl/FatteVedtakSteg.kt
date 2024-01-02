package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class FatteVedtakSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepositoryImpl
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        if (avklaringsbehov.skalTilbakeføresEtterTotrinnsVurdering()) {
            return StegResultat(tilbakeførtFraBeslutter = true)
        }
        if (avklaringsbehov.harHattAvklaringsbehovSomHarKrevdToTrinn()) {
            return StegResultat(listOf(Definisjon.FATTE_VEDTAK))
        }

        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return FatteVedtakSteg(AvklaringsbehovRepositoryImpl(connection))
        }

        override fun type(): StegType {
            return StegType.FATTE_VEDTAK
        }
    }
}
