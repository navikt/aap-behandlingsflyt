package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.brev.BrevUtlederService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.SKRIV_BREV
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder

class SkrivBrevSteg private constructor(
    private val brevUtlederService: BrevUtlederService,
    private val brevbestillingService: BrevbestillingService
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val brevBehov = brevUtlederService.utledBrevbehov(kontekst.behandlingId)
        if (brevBehov.harBehovForBrev()) {
            val typeBrev = brevBehov.typeBrev!!
            val eksisterendeBestilling =
                brevbestillingService.eksisterendeBestilling(kontekst.behandlingId, typeBrev)
            return if (eksisterendeBestilling?.status == Status.FORHÅNDSVISNING_KLAR) {
                StegResultat(listOf(SKRIV_BREV))
            } else {
                StegResultat(listOf(SKRIV_BREV))
            }
        }
        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return SkrivBrevSteg(BrevUtlederService.konstruer(connection), BrevbestillingService.konstruer(connection))
        }

        override fun type(): StegType {
            return StegType.SKRIV_BREV
        }
    }
}
