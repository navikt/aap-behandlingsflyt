package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.brev.BrevUtlederService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.AVVENTER_BREV_BESTILLING
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
            val eksisterendeBestilling =
                brevbestillingService.eksisterendeBestilling(kontekst.behandlingId)
            if (eksisterendeBestilling == null) {
                // Bestill hvis ikke bestilt allerede
                brevbestillingService.bestill(kontekst.behandlingId, brevBehov.typeBrev!!)
                return StegResultat(listOf(AVVENTER_BREV_BESTILLING))
            }

            val status = brevbestillingService.oppdaterStatus(kontekst.behandlingId)

            // Er bestilling klar for visning
            return when (status) {
                // hvis ikke gå på vent
                Status.SENDT -> StegResultat(listOf(AVVENTER_BREV_BESTILLING))
                // hvis klar gi avklaringsbehov for brevskriving
                Status.FORHÅNDSVISNING_KLAR -> StegResultat() // TODO StegResultat(listOf(SKRIV_BREV)) ?
                // er brevet fullført, iverksett og gå videre til avslutting av behandling
                Status.FULLFØRT -> StegResultat()
            }
        }
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return BrevSteg(BrevUtlederService(), BrevbestillingService.konstruer(connection))
        }

        override fun type(): StegType {
            return StegType.BREV
        }
    }
}