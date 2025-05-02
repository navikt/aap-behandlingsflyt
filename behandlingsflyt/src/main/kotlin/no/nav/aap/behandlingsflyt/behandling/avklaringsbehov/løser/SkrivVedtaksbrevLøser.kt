package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivVedtaksbrevLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.SignaturService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryRegistry

class SkrivVedtaksbrevLøser(val connection: DBConnection) : AvklaringsbehovsLøser<SkrivVedtaksbrevLøsning> {

    private val repositoryProvider = RepositoryRegistry.provider(connection)
    private val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
    private val sakRepository = repositoryProvider.provide<SakRepository>()
    private val brevbestillingRepository = repositoryProvider.provide<BrevbestillingRepository>()
    private val avklaringsbehovRepository = repositoryProvider.provide<AvklaringsbehovRepository>()

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: SkrivVedtaksbrevLøsning
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
            SkrivVedtaksbrevLøsning.Handling.FERDIGSTILL -> {
                brevbestillingService.ferdigstill(kontekst.behandlingId(), brevbestillingReferanse, kontekst.bruker)
                LøsningsResultat("Brev ferdig")
            }

            SkrivVedtaksbrevLøsning.Handling.AVBRYT -> {
                brevbestillingService.avbryt(kontekst.behandlingId(), brevbestillingReferanse)
                LøsningsResultat("Brev avbrutt")
            }
        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SKRIV_VEDTAKSBREV
    }
}
