package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class SkrivBrevLøser(val connection: DBConnection) : AvklaringsbehovsLøser<SkrivBrevLøsning> {

    private val repositoryProvider = RepositoryProvider(connection)
    private val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
    private val sakRepository = repositoryProvider.provide<SakRepository>()
    private val brevbestillingRepository = repositoryProvider.provide<BrevbestillingRepository>()

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: SkrivBrevLøsning
    ): LøsningsResultat {
        val brevbestillingService = BrevbestillingService(
            brevbestillingGateway = GatewayProvider.provide(),
            brevbestillingRepository = brevbestillingRepository,
            behandlingRepository = behandlingRepository,
            sakRepository = sakRepository
        )
        val brevbestillingReferanse = BrevbestillingReferanse(løsning.brevbestillingReferanse)
        return when (løsning.handling) {
            SkrivBrevLøsning.Handling.FERDIGSTILL -> {
                brevbestillingService.ferdigstill(kontekst.behandlingId(), brevbestillingReferanse)
                LøsningsResultat("Brev ferdig")
            }

            SkrivBrevLøsning.Handling.AVBRYT -> {
                brevbestillingService.avbryt(kontekst.behandlingId(), brevbestillingReferanse)
                LøsningsResultat("Brev avbrutt")
            }
        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SKRIV_BREV
    }
}