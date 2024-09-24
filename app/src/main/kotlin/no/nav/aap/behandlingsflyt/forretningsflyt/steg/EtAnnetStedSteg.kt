package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetStedUtlederService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class EtAnnetStedSteg(
    private val etAnnetStedUtlederService: EtAnnetStedUtlederService
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        val behovForAvklaringer = etAnnetStedUtlederService.harBehovForAvklaringer(behandlingId = kontekst.behandlingId)

        if(behovForAvklaringer.harBehov()) {
            return StegResultat(behovForAvklaringer.avklaringsbehov())
        }

        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return EtAnnetStedSteg(
                EtAnnetStedUtlederService(connection)
            )
        }

        override fun type(): StegType {
            return StegType.DU_ER_ET_ANNET_STED
        }
    }
}