package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningRegelService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import org.slf4j.LoggerFactory

class SamordningSteg(private val samordningRegelService: SamordningRegelService) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(SamordningSteg::class.java)
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val samordningTidslinje = samordningRegelService.vurder(kontekst.behandlingId)

        log.info("Samordning tidslinje $samordningTidslinje")
        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return SamordningSteg(
                SamordningRegelService()
            )
        }

        override fun type(): StegType {
            return StegType.SAMORDNING_GRADERING
        }
    }

}