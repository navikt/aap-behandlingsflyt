package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.behandling.brev.BrevBehov
import no.nav.aap.behandlingsflyt.behandling.brev.SignaturService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.MottakerDto
import no.nav.aap.brev.kontrakt.Vedlegg
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import java.util.*

class BrevbestillingService(
    private val signaturService: SignaturService,
    private val brevbestillingGateway: BrevbestillingGateway,
    private val brevbestillingRepository: BrevbestillingRepository,
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
) {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        signaturService = SignaturService(repositoryProvider),
        brevbestillingGateway = gatewayProvider.provide(),
        brevbestillingRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
    )

    fun harBestillingOmVedtak(behandlingId: BehandlingId): Boolean {
        return brevbestillingRepository.hent(behandlingId).any { it.typeBrev.erVedtak() }
    }

    fun erAlleBestillingerOmVedtakIEndeTilstand(behandlingId: BehandlingId): Boolean {
        val vedtakBestillinger = brevbestillingRepository.hent(behandlingId).filter { it.typeBrev.erVedtak() }
        return vedtakBestillinger.all { it.status.erEndeTilstand() }
    }

    fun hentTilbakestillbareBestillingerOmVedtak(behandlingId: BehandlingId): List<Brevbestilling> {
        val vedtakBestillinger = brevbestillingRepository.hent(behandlingId).filter { it.typeBrev.erVedtak() }
        return vedtakBestillinger.filter { !it.status.erEndeTilstand() }
    }

    fun hentBestillinger(behandlingId: BehandlingId, typeBrev: TypeBrev): List<Brevbestilling> {
        val bestillinger = brevbestillingRepository.hent(behandlingId).filter { it.typeBrev == typeBrev }
        return bestillinger
    }

    fun bestill(
        behandlingId: BehandlingId,
        brevBehov: BrevBehov,
        unikReferanse: String,
        ferdigstillAutomatisk: Boolean,
        vedlegg: Vedlegg? = null,
        brukApiV3: Boolean = false,
    ): UUID {
        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)
        val bestillingReferanse = brevbestillingGateway.bestillBrev(
            saksnummer = sak.saksnummer,
            brukerIdent = sak.person.aktivIdent(),
            behandlingReferanse = behandling.referanse,
            unikReferanse = unikReferanse,
            brevBehov = brevBehov,
            vedlegg = vedlegg,
            ferdigstillAutomatisk = ferdigstillAutomatisk,
            brukApiV3 = brukApiV3,
        )

        val status = if (ferdigstillAutomatisk) {
            Status.FULLFØRT
        } else {
            Status.FORHÅNDSVISNING_KLAR
        }

        brevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = brevBehov.typeBrev,
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
            throw UgyldigForespørselException("Brevet er ikke gyldig ferdigstilt, fullfør brevet og prøv på nytt.")
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

    fun gjenopptaBestilling(behandlingId: BehandlingId, referanse: BrevbestillingReferanse) {
        brevbestillingGateway.gjenoppta(referanse)
        brevbestillingRepository.oppdaterStatus(
            behandlingId = behandlingId,
            referanse = referanse,
            status = Status.FORHÅNDSVISNING_KLAR
        )
    }
}
