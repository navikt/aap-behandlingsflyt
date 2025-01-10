package no.nav.aap.behandlingsflyt.flyt.testutil

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Brevbestilling
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingRepository
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.atomic.AtomicLong

object InMemoryBrevbestillingRepository: BrevbestillingRepository {
    private val bestilling = mutableListOf<Brevbestilling>()
    private val id = AtomicLong()

    override fun hent(behandlingId: BehandlingId): List<Brevbestilling> {
        return bestilling.filter { it.behandlingId == behandlingId }.toList()
    }

    override fun lagre(
        behandlingId: BehandlingId,
        typeBrev: TypeBrev,
        bestillingReferanse: BrevbestillingReferanse,
        status: Status
    ) {
        bestilling += Brevbestilling(
            id = id.getAndIncrement(),
            behandlingId = behandlingId,
            typeBrev = typeBrev,
            referanse = bestillingReferanse,
            status = status,
        )
    }

    override fun oppdaterStatus(behandlingId: BehandlingId, referanse: BrevbestillingReferanse, status: Status) {
        val index = bestilling.indexOfFirst { it.referanse == referanse }
        bestilling[index] = bestilling[index].copy(status = status)
    }
}