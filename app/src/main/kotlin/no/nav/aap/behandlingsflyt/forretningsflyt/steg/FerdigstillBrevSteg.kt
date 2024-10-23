package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.brev.BrevUtlederService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class FerdigstillBrevSteg private constructor(
    private val brevUtlederService: BrevUtlederService,
    private val brevbestillingService: BrevbestillingService
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val brevBehov = brevUtlederService.utledBrevbehov(kontekst.behandlingId)
        if (brevBehov.harBehovForBrev()) {
            val typeBrev = brevBehov.typeBrev!!
            val bestilling =
                brevbestillingService.eksisterendeBestilling(kontekst.behandlingId, typeBrev)
            if (bestilling == null || bestilling.status != Status.FORHÅNDSVISNING_KLAR) {
                return StegResultat() // TODO ? Det er behov for brev men brevbestilling mangler eller har en uventet status på dette steget
            }
            if (brevbestillingService.ferdigstill(bestilling.referanse)) {
                return StegResultat()
            }
            return StegResultat() // TODO validering av bestilling feiler. Tilbake til at saksbehandler må gjøre endringer i brevet
        }
        return StegResultat()
    }
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return FerdigstillBrevSteg(BrevUtlederService.konstruer(connection), BrevbestillingService.konstruer(connection))
        }

        override fun type(): StegType {
            return StegType.FERDIGSTILL_BREV
        }
    }
}
