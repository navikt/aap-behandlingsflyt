package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class SkrivBrevAvklaringsbehovLøser(
    private val brevbestillingService: BrevbestillingService
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        brevbestillingService = BrevbestillingService(repositoryProvider, gatewayProvider)
    )

    fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: SkrivBrevAvklaringsbehovLøsning
    ): LøsningsResultat {
        val brevbestillingReferanse = BrevbestillingReferanse(løsning.brevbestillingReferanse)

        val begrunnelse = løsning.begrunnelse?.let { ".\n $it" } ?: ""

        return when (løsning.handling) {
            SkrivBrevAvklaringsbehovLøsning.Handling.FERDIGSTILL -> {
                brevbestillingService.ferdigstill(
                    kontekst.behandlingId(),
                    brevbestillingReferanse,
                    kontekst.bruker,
                    løsning.mottakere
                )

                LøsningsResultat("Brev ferdig${begrunnelse}")
            }

            SkrivBrevAvklaringsbehovLøsning.Handling.AVBRYT -> {
                brevbestillingService.avbryt(kontekst.behandlingId(), brevbestillingReferanse)
                LøsningsResultat("Brev avbrutt${begrunnelse}")
            }
        }
    }
}
