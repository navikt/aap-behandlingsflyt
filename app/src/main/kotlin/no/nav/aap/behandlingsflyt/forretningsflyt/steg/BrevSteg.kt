package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.brev.BrevUtlederService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class BrevSteg private constructor(
    private val brevUtlederService: BrevUtlederService,
    private val brevbestillingService: BrevbestillingService
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val brevBehov = brevUtlederService.utledBrevbehov(kontekst.behandlingId)
        if (brevBehov.harBehovForBrev()) {
            // Bestill hvis ikke bestilt allerede
            // Er bestilling klar for visning
            // hvis ikke gå på vent
            // hvis klar gi avklaringsbehov for brevskriving
            // er brevet fullført, iverksett og gå videre til avslutting av behandling
        }
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return BrevSteg(BrevUtlederService(), BrevbestillingService())
        }

        override fun type(): StegType {
            return StegType.BREV
        }
    }
}