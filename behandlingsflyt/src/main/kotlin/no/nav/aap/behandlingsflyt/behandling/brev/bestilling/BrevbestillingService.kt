package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.behandling.brev.SignaturService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.Faktagrunnlag
import no.nav.aap.brev.kontrakt.MottakerDto
import no.nav.aap.brev.kontrakt.Vedlegg
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import java.util.*

class BrevbestillingService(
    private val signaturService: SignaturService,
    private val brevbestillingGateway: BrevbestillingGateway,
    private val brevbestillingRepository: BrevbestillingRepository,
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        signaturService = SignaturService(repositoryProvider),
        brevbestillingGateway = GatewayProvider.provide(),
        brevbestillingRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
    )

    fun harBestillingOmVedtak(behandlingId: BehandlingId): Boolean {
        return brevbestillingRepository.hent(behandlingId).any { it.typeBrev.erVedtak() }
    }

    fun hentBestillinger(behandlingId: BehandlingId, typeBrev: TypeBrev): List<Brevbestilling> {
        val bestillinger = brevbestillingRepository.hent(behandlingId).filter { it.typeBrev == typeBrev }
        return bestillinger
    }

    fun bestillV2(
        behandlingId: BehandlingId,
        typeBrev: TypeBrev,
        unikReferanse: String,
        ferdigstillAutomatisk: Boolean,
        faktagrunnlag: Set<Faktagrunnlag> = emptySet(),
        vedlegg: Vedlegg? = null
    ): UUID {
        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)

        val bestillingReferanse = brevbestillingGateway.bestillBrevV2(
            saksnummer = sak.saksnummer,
            brukerIdent = sak.person.aktivIdent(),
            behandlingReferanse = behandling.referanse,
            unikReferanse = unikReferanse,
            typeBrev = typeBrev,
            vedlegg = vedlegg,
            faktagrunnlag = faktagrunnlag,
            ferdigstillAutomatisk = ferdigstillAutomatisk,
        )

        val status = if (ferdigstillAutomatisk) {
            Status.FULLFØRT
        } else {
            Status.FORHÅNDSVISNING_KLAR
        }

        brevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = typeBrev,
            bestillingReferanse = bestillingReferanse,
            status = status,
        )
        return bestillingReferanse.brevbestillingReferanse
    }

    fun hentBrevbestilling(referanse: BrevbestillingReferanse): BrevbestillingResponse {
        return brevbestillingGateway.hent(referanse)
    }

    fun hentBrevbestillinger(behandlingReferanse: BehandlingReferanse): List<Brevbestilling> {
        val behandling = behandlingRepository.hent(behandlingReferanse)
        return brevbestillingRepository.hent(behandling.id)
    }

    fun oppdaterStatus(behandlingId: BehandlingId, referanse: BrevbestillingReferanse, status: Status) {
        brevbestillingRepository.oppdaterStatus(behandlingId, referanse, status)
    }

    /**
     * @param mottakere Dersom tom settes brukeren brevet gjelder automatisk som mottaker
     */
    fun ferdigstill(
        behandlingId: BehandlingId,
        brevbestillingReferanse: BrevbestillingReferanse,
        bruker: Bruker,
        mottakere: List<MottakerDto> = emptyList()
    ) {
        val brevbestilling = brevbestillingRepository.hent(brevbestillingReferanse)
        val signaturer = signaturService.finnSignaturGrunnlag(brevbestilling, bruker)
        val ferdigstilt = brevbestillingGateway.ferdigstill(brevbestillingReferanse, signaturer, mottakere)
        if (!ferdigstilt) {
            throw IllegalArgumentException("Brevet er ikke gyldig ferdigstilt, fullfør brevet og prøv på nytt.")
        } else {
            brevbestillingRepository.oppdaterStatus(
                behandlingId = behandlingId,
                referanse = brevbestillingReferanse,
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
