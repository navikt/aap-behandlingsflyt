package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.barnetillegg.BarnetilleggService
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.sakRepository
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.flyt.StegType
import org.slf4j.LoggerFactory

class BarnetilleggSteg(private val barnetilleggService: BarnetilleggService) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(BarnetilleggSteg::class.java)

    override fun utfør(kontekst: FlytKontekst): StegResultat {

        val barnetillegg = barnetilleggService.beregn(kontekst.behandlingId)

        log.info("Barnetillegg {}", barnetillegg)

        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return BarnetilleggSteg(
                BarnetilleggService(
                    BarnRepository(connection),
                    behandlingRepository(connection),
                    sakRepository(connection)
                )
            )
        }

        override fun type(): StegType {
            return StegType.BARNETILLEGG
        }
    }
}