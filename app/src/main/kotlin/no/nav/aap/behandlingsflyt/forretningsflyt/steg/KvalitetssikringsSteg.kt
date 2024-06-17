package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType

class KvalitetssikringsSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        if (avklaringsbehov.skalTilbakeføresEtterKvalitetssikring()) {
            return StegResultat(tilbakeførtFraKvalitetssikrer = true)
        }
        if (avklaringsbehov.harHattAvklaringsbehovSomKreverKvalitetssikring()) {
            return StegResultat(listOf(Definisjon.KVALITETSSIKRING))
        }

        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return KvalitetssikringsSteg(AvklaringsbehovRepositoryImpl(connection))
        }

        override fun type(): StegType {
            return StegType.KVALITETSSIKRING
        }
    }
}
