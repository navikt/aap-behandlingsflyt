package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.Vedlegg
import java.util.*

class BrevbestillingService(
    private val brevbestillingGateway: BrevbestillingGateway,
    private val brevbestillingRepository: BrevbestillingRepository,
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
) {

    fun harBestillingOmVedtak(behandlingId: BehandlingId): Boolean {
        return brevbestillingRepository.hent(behandlingId).any { it.typeBrev.erVedtak() }
    }

    fun hentBestillinger(behandlingId: BehandlingId, typeBrev: TypeBrev): List<Brevbestilling> {
        val bestillinger = brevbestillingRepository.hent(behandlingId).filter { it.typeBrev == typeBrev }
        return bestillinger
    }

    fun bestill(behandlingId: BehandlingId, typeBrev: TypeBrev, unikReferanse: String, vedlegg: Vedlegg? = null): UUID {
        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)

        val bestillingReferanse = brevbestillingGateway.bestillBrev(
            saksnummer = sak.saksnummer,
            behandlingReferanse = behandling.referanse,
            unikReferanse = unikReferanse,
            typeBrev = typeBrev,
            vedlegg = vedlegg
        )
        brevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = typeBrev,
            bestillingReferanse = bestillingReferanse,
            status = Status.SENDT,
        )
        return bestillingReferanse.brevbestillingReferanse
    }

    fun hentBrevbestilling(referanse: BrevbestillingReferanse): BrevbestillingResponse {
        return brevbestillingGateway.hent(referanse)
    }

    fun hentBrevbestillinger(behandlingReferanse: BehandlingReferanse): List<BrevbestillingResponse> {
        val behandling = behandlingRepository.hent(behandlingReferanse)
        return brevbestillingRepository.hent(behandling.id).map {
            hentBrevbestilling(it.referanse)
        }
    }

    fun oppdaterStatus(behandlingId: BehandlingId, referanse: BrevbestillingReferanse, status: Status) {
        brevbestillingRepository.oppdaterStatus(behandlingId, referanse, status)
    }

    fun ferdigstill(behandlingId: BehandlingId, referanse: BrevbestillingReferanse) {
        val ferdigstilt = brevbestillingGateway.ferdigstill(referanse)
        if (!ferdigstilt) {
            throw IllegalArgumentException("Brevet er ikke gyldig ferdigstilt, fullfør brevet og prøv på nytt.")
        } else {
            brevbestillingRepository.oppdaterStatus(
                behandlingId = behandlingId,
                referanse = referanse,
                status = Status.FULLFØRT
            )
        }
    }

    fun avbryt(behandlingId: BehandlingId, referanse: BrevbestillingReferanse) {
        brevbestillingGateway.avbryt(referanse)
        brevbestillingRepository.oppdaterStatus(
            behandlingId = behandlingId,
            referanse = referanse,
            status = Status.AVBRUTT
        )
    }
}
