package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.brev.BrevUtlederService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.kontrakt.brev.Status
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.AVVENTER_BREV_BESTILLING
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.SKRIV_BREV
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
            val typeBrev = brevBehov.typeBrev!!
            val eksisterendeBestilling =
                brevbestillingService.eksisterendeBestilling(kontekst.behandlingId, typeBrev)
            if (eksisterendeBestilling == null) {
                // Bestill hvis ikke bestilt allerede
                brevbestillingService.bestill(kontekst.behandlingId, typeBrev)
                return StegResultat(listOf(AVVENTER_BREV_BESTILLING))
            }

            // Er bestilling klar for visning
            return when (eksisterendeBestilling.status) {
                // hvis ikke gå på vent
                Status.SENDT -> StegResultat(listOf(AVVENTER_BREV_BESTILLING))
                // hvis klar gi avklaringsbehov for brevskriving
                Status.FORHÅNDSVISNING_KLAR -> StegResultat(listOf(SKRIV_BREV))
                // er brevet fullført, iverksett og gå videre til avslutting av behandling
                Status.FULLFØRT -> StegResultat()
            }
        }
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return BrevSteg(BrevUtlederService.konstruer(connection), BrevbestillingService.konstruer(connection))
        }

        override fun type(): StegType {
            return StegType.BREV
        }
    }
}