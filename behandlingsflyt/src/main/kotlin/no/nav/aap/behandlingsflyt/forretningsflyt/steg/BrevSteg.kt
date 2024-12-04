package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.behandlingsflyt.behandling.brev.BrevUtlederService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FantAvklaringsbehov
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.Ventebehov
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.BESTILL_BREV
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.SKRIV_BREV
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.dbconnect.DBConnection

class BrevSteg private constructor(
    private val brevUtlederService: BrevUtlederService,
    private val brevbestillingService: BrevbestillingService
) : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        val brevBehov = brevUtlederService.utledBrevbehov(kontekst.behandlingId)
        if (brevBehov.harBehovForBrev()) {
            val typeBrev = brevBehov.typeBrev!!
            val bestilling =
                brevbestillingService.eksisterendeBestilling(kontekst.behandlingId, typeBrev)
            if (bestilling == null) {
                // Bestill hvis ikke bestilt allerede
                brevbestillingService.bestill(kontekst.behandlingId, typeBrev)
                return FantVentebehov(Ventebehov(BESTILL_BREV, ÅrsakTilSettPåVent.VENTER_PÅ_MASKINELL_AVKLARING))
            }

            // Er bestilling klar for visning
            return when (bestilling.status) {
                // hvis ikke gå på vent
                Status.SENDT ->
                    FantVentebehov(Ventebehov(BESTILL_BREV, ÅrsakTilSettPåVent.VENTER_PÅ_MASKINELL_AVKLARING))
                // hvis klar gi avklaringsbehov for brevskriving
                Status.FORHÅNDSVISNING_KLAR -> FantAvklaringsbehov(SKRIV_BREV)
                // er brevet fullført, iverksett og gå videre til avslutting av behandling
                Status.FULLFØRT -> {
                    val ferdigstilt = brevbestillingService.ferdigstill(bestilling.referanse)
                    if (!ferdigstilt) {
                        // Validering har feilet så saksbehandler må gjøre endringer
                        brevbestillingService.oppdaterStatus(
                            behandlingId = kontekst.behandlingId,
                            referanse = bestilling.referanse,
                            status = Status.FORHÅNDSVISNING_KLAR
                        )
                        FantAvklaringsbehov(SKRIV_BREV)
                    } else {
                        Fullført
                    }
                }
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