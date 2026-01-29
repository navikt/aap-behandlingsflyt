package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.*
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

object InMemoryBrevbestillingRepository: BrevbestillingRepository {
    private val bestilling = CopyOnWriteArrayList<Brevbestilling>()
    private val id = AtomicLong()
    private val lock = Object()

    override fun hent(
        sakId: SakId,
        typeBrev: TypeBrev
    ): List<Brevbestilling> {
        val behandlinger = InMemoryBehandlingRepository.hentAlleFor(sakId).map { it.id }
        return bestilling.filter { behandlinger.contains(it.behandlingId) }
    }

    override fun hent(behandlingId: BehandlingId): List<Brevbestilling> {
        synchronized(lock) {
            return bestilling.filter { it.behandlingId == behandlingId }.toList()
        }
    }

    override fun hent(brevbestillingReferanse: BrevbestillingReferanse): Brevbestilling? {
        synchronized(lock) {
            return bestilling.firstOrNull { it.referanse == brevbestillingReferanse }
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        typeBrev: TypeBrev,
        bestillingReferanse: BrevbestillingReferanse,
        status: Status
    ) {
        synchronized(lock) {
            bestilling.add(Brevbestilling(
                id = id.getAndIncrement(),
                behandlingId = behandlingId,
                typeBrev = typeBrev,
                referanse = bestillingReferanse,
                status = status,
                opprettet = LocalDateTime.now(),
            ))
        }
    }

    override fun oppdaterStatus(behandlingId: BehandlingId, referanse: BrevbestillingReferanse, status: Status) {
        synchronized(lock) {
            val index = bestilling.indexOfFirst { it.referanse == referanse }
            if (index >= 0) {
                bestilling[index] = bestilling[index].copy(status = status)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
    }

    fun clearMemory() {
        bestilling.clear()
    }

    override fun hentBehandlingsreferanseForBestilling(referanse: BrevbestillingReferanse): BehandlingReferanse {
        TODO("Not yet implemented")
    }
}
