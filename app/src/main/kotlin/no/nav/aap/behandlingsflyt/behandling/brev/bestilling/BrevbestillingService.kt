package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

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

    fun oppdaterStatus(behandlingId: BehandlingId, referanse: BrevbestillingReferanse, status: Status) {
        brevbestillingRepository.oppdaterStatus(behandlingId, referanse, status)
    }

    fun hentSisteBrevbestilling(behandlingReferanse: BehandlingReferanse): BrevbestillingResponse? {
        val behandling = behandlingRepository.hent(behandlingReferanse)

        val brevbestilling = // TODO Bør ha en mer robust logikk for å finne relevant brev for editering, gitt en behandlingreferanse
            brevbestillingRepository.hent(behandling.id).sortedByDescending { it.id }.first()

        return brevbestillingGateway.hent(brevbestilling.referanse)
    }

    fun ferdigstill(referanse: BrevbestillingReferanse): Boolean {
        return brevbestillingGateway.ferdigstill(referanse)
    }
}
