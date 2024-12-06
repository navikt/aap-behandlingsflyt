package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.brev.kontrakt.BrevbestillingResponse

class BrevbestillingService(
    private val brevbestillingGateway: BrevbestillingGateway,
    private val brevbestillingRepository: BrevbestillingRepository,
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
) {

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

        val brevbestilling =
            // TODO Bør ha en mer robust logikk for å finne relevant brev for editering, gitt en behandlingreferanse
            brevbestillingRepository.hent(behandling.id).maxByOrNull { it.id }!!

        return brevbestillingGateway.hent(brevbestilling.referanse)
    }

    fun ferdigstill(referanse: BrevbestillingReferanse): Boolean {
        return brevbestillingGateway.ferdigstill(referanse)
    }
}
