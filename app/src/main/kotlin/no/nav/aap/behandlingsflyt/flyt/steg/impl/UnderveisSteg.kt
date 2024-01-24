package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.PliktkortRepository
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.verdityper.flyt.StegType
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.underveis.UnderveisService
import org.slf4j.LoggerFactory

class UnderveisSteg(private val underveisService: UnderveisService) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(UnderveisSteg::class.java)

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val underveisTidslinje = underveisService.vurder(kontekst.behandlingId)

        log.info("Underveis tidslinje $underveisTidslinje")

        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return UnderveisSteg(
                UnderveisService(
                    VilkårsresultatRepository(connection),
                    PliktkortRepository(connection)
                )
            )
        }

        override fun type(): StegType {
            return StegType.FASTSETT_UTTAK
        }
    }
}