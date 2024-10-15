package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class BrevbestillingService(
    private val brevbestillingGateway: BrevbestillingGateway,
    private val brevbestillingRepository: BrevbestillingRepository,
    private val behandlingRepository: BehandlingRepository,
) {

    companion object {
        fun konstruer(connection: DBConnection): BrevbestillingService {
            return BrevbestillingService(
                BrevGateway(),
                BrevbestillingRepository(connection),
                BehandlingRepositoryImpl(connection)
            )
        }
    }

    fun eksisterendeBestilling(behandlingId: BehandlingId, typeBrev: TypeBrev): Brevbestilling? {
        return brevbestillingRepository.hent(behandlingId, typeBrev)
    }

    fun bestill(behandlingId: BehandlingId, typeBrev: TypeBrev) {
        val behandlingReferanse = behandlingRepository.hent(behandlingId).referanse
        val bestillingReferanse = brevbestillingGateway.bestillBrev(behandlingReferanse, typeBrev)
        brevbestillingRepository.lagre(
            behandlingId,
            typeBrev,
            bestillingReferanse,
            Status.SENDT,
        )
    }
}
