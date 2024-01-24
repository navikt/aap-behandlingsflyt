package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.barnetillegg.BarnetilleggService
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.barn.BarnRepository
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.verdityper.flyt.StegType
import org.slf4j.LoggerFactory

class BarnetilleggSteg(private val barnetilleggService: BarnetilleggService) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(BarnetilleggSteg::class.java)

    override fun utfør(kontekst: FlytKontekst): StegResultat {

        val barnetillegg = barnetilleggService.beregn(kontekst.behandlingId)
        // TODO: Disjoint for perioden med rett

        log.info("Barnetillegg {}", barnetillegg)

        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return BarnetilleggSteg(BarnetilleggService(BarnRepository(connection)))
        }

        override fun type(): StegType {
            return StegType.BARNETILLEGG
        }
    }
}