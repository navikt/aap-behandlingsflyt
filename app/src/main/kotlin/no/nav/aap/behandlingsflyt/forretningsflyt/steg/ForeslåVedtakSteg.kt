package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class ForeslåVedtakSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepositoryImpl
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)

        if (avklaringsbehov.harHattAvklaringsbehov() && avklaringsbehov.harIkkeForeslåttVedtak()) {
            return StegResultat(listOf(Definisjon.FORESLÅ_VEDTAK))
        }

        return StegResultat() // DO NOTHING
    }

    override fun vedTilbakeføring(kontekst: FlytKontekstMedPerioder) {
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId)
        val relevanteBehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FORESLÅ_VEDTAK))

        if (relevanteBehov.isNotEmpty()) {
            avklaringsbehovene.avbryt(Definisjon.FORESLÅ_VEDTAK)
        }
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return ForeslåVedtakSteg(AvklaringsbehovRepositoryImpl(connection))
        }

        override fun type(): StegType {
            return StegType.FORESLÅ_VEDTAK
        }
    }
}
