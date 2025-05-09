package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.SignaturService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class SkrivBrevAvklaringsbehovLøser(
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val brevbestillingRepository: BrevbestillingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        brevbestillingRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide(),
    )

    fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: SkrivBrevAvklaringsbehovLøsning
    ): LøsningsResultat {
        val brevbestillingService = BrevbestillingService(
            signaturService = SignaturService(avklaringsbehovRepository = avklaringsbehovRepository),
            brevbestillingGateway = GatewayProvider.provide<BrevbestillingGateway>(),
            brevbestillingRepository = brevbestillingRepository,
            behandlingRepository = behandlingRepository,
            sakRepository = sakRepository
        )
        val brevbestillingReferanse = BrevbestillingReferanse(løsning.brevbestillingReferanse)
        return when (løsning.handling) {
            SkrivBrevAvklaringsbehovLøsning.Handling.FERDIGSTILL -> {
                brevbestillingService.ferdigstill(kontekst.behandlingId(), brevbestillingReferanse, kontekst.bruker)
                LøsningsResultat("Brev ferdig")
            }

            SkrivBrevAvklaringsbehovLøsning.Handling.AVBRYT -> {
                brevbestillingService.avbryt(kontekst.behandlingId(), brevbestillingReferanse)
                LøsningsResultat("Brev avbrutt")
            }
        }
    }
}
