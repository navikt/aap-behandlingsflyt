package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class ForeslåVedtakSteg private constructor(
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val avklaringsbehov = avklaringsbehovRepository.hent(kontekst.behandlingId)

        if (avklaringsbehov.harHattAvklaringsbehov() && avklaringsbehov.harIkkeForeslåttVedtak()) {
            return StegResultat(listOf(Definisjon.FORESLÅ_VEDTAK))
        }

        return StegResultat() // DO NOTHING
    }

    override fun vedTilbakeføring(kontekst: FlytKontekst) {
        val avklaringsbehovene = avklaringsbehovRepository.hent(kontekst.behandlingId)
        val relevanteBehov = avklaringsbehovene.hentBehovForDefinisjon(listOf(Definisjon.FORESLÅ_VEDTAK))

        if (relevanteBehov.isNotEmpty()) {
            avklaringsbehovene.avbryt(Definisjon.FORESLÅ_VEDTAK)

            //TODO: Bør logikk legges i domenet eller repository?
            // Et argument for å ikke legge for mye logikk i selve Repository, er at vi kan teste domenet uten å koble på repository.
            //Repository bør først og fremst være dumt, og sikre lagring. Ikke så mye annet.
            //Dersom det er riktig så må lagringen greie å identifisere hva som er lagret ned fra før, og hva som er endret.
            //avklaringsbehovRepository.lagre(avklaringsbehovene)
        }
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return ForeslåVedtakSteg(AvklaringsbehovRepository(connection))
        }

        override fun type(): StegType {
            return StegType.FORESLÅ_VEDTAK
        }
    }
}
