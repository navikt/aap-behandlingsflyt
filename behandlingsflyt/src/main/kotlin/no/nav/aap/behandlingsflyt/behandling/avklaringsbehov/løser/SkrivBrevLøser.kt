package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class SkrivBrevLøser(val connection: DBConnection) : AvklaringsbehovsLøser<SkrivBrevLøsning> {

    private val repositoryProvider = RepositoryProvider(connection)
    private val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
    private val sakRepository = repositoryProvider.provide<SakRepository>()

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: SkrivBrevLøsning
    ): LøsningsResultat {
        val brevbestillingRepository = BrevbestillingRepositoryImpl(connection)
        val brevbestillingService = BrevbestillingService(
            brevbestillingGateway = BrevGateway(),
            brevbestillingRepository = brevbestillingRepository,
            behandlingRepository = behandlingRepository,
            sakRepository = sakRepository
        )
        val brevbestillingReferanse = BrevbestillingReferanse(løsning.brevbestillingReferanse)
        val ferdigstilt = brevbestillingService.ferdigstill(brevbestillingReferanse)
        if (!ferdigstilt) {
            throw IllegalArgumentException("Brevet er ikke gyldig ferdigstilt, fullfør brevet og prøv på nytt.")
        } else {
            brevbestillingRepository.oppdaterStatus(
                behandlingId = kontekst.behandlingId(),
                referanse = brevbestillingReferanse,
                status = Status.FULLFØRT
            )
        }

        return LøsningsResultat("Brev ferdig")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SKRIV_VEDTAK_AVSLAG_BREV // TODO en løser for hver "skriv brev"-definisjon?
    }
}