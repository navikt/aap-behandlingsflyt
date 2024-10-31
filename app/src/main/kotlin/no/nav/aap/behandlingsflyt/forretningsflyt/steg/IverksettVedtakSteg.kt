package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class IverksettVedtakSteg private constructor() : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        // TODO: Melding om vedtak
        // TODO: Iverksette mot økonomi

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return IverksettVedtakSteg()
        }

        override fun type(): StegType {
            return StegType.IVERKSETT_VEDTAK
        }
    }
}
