package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.util.UUID

class BrevbestillingService(
    private val brevbestillingGateway: BrevbestillingGateway,
    private val brevbestillingRepository: BrevbestillingRepository,
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
) {

    companion object {
        fun konstruer(connection: DBConnection): BrevbestillingService {
            return BrevbestillingService(
                BrevGateway(),
                BrevbestillingRepository(connection),
                BehandlingRepositoryImpl(connection),
                SakRepositoryImpl(connection)
            )
        }
    }

    fun eksisterendeBestilling(behandlingId: BehandlingId, typeBrev: TypeBrev): Brevbestilling? {
        return brevbestillingRepository.hent(behandlingId, typeBrev)
    }

    fun bestill(behandlingId: BehandlingId, typeBrev: TypeBrev) {
        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)
        val bestillingReferanse = brevbestillingGateway.bestillBrev(
            saksnummer = sak.saksnummer,
            behandlingReferanse = behandling.referanse,
            typeBrev = typeBrev
        )
        brevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = typeBrev,
            bestillingReferanse = bestillingReferanse,
            status = Status.SENDT,
        )
    }

    fun oppdaterStatus(behandlingId: BehandlingId, referanse: UUID, status: Status) {
        brevbestillingRepository.oppdaterStatus(behandlingId, referanse, status)
    }

    fun ferdigstill(referanse: UUID): Boolean {
        return brevbestillingGateway.ferdigstill(referanse)
    }
}
