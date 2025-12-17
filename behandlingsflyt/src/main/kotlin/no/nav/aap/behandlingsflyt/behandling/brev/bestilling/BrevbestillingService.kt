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

    fun erNyesteBestillingerOmVedtakIEndeTilstand(behandlingId: BehandlingId): Boolean {
        val nyeste = brevbestillingRepository.hent(behandlingId)
            .filter { it.typeBrev.erVedtak() }
            .maxByOrNull { it.opprettet }
        return nyeste == null || nyeste.status.erEndeTilstand()
    }

    fun erNyesteBestillingOmVedtakIEndeTilstandavklaringsbehov(behandlingId: BehandlingId, typeBrev: TypeBrev): Boolean {
        val bestilling = hentNyesteAktiveBestilling(behandlingId, typeBrev)
        return bestilling != null && bestilling.status.erEndeTilstand()
    }

    fun erBrevBestillingIEndeTilstand(brevbestillingReferanse: UUID): Boolean {
        val bestilling = hentBrevbestilling(brevbestillingReferanse)
        return bestilling != null && bestilling.status.erEndeTilstand()
    }

    fun gjenopptaVedtakBrevBestillinger(behandlingId: BehandlingId)  {
        val bestillinger = brevbestillingRepository.hent(behandlingId).filter { it.typeBrev.erVedtak() }
        bestillinger
            .filter { it.status.kanGjenopptas() }
            .forEach {
                gjenopptaBestilling(it.behandlingId, it.referanse)
            }
    }

    fun hentBestillinger(behandlingId: BehandlingId, typeBrev: TypeBrev): List<Brevbestilling> {
        val bestillinger = brevbestillingRepository.hent(behandlingId).filter { it.typeBrev == typeBrev }
        return bestillinger
    }

    /**
     * I dagens løsning for brevbestilling ønskes et 1-1 forhold mellom behandlingId og typeBrev
     * Dvs. det eksisterer til en hver tid kun 1 bestilling per brevtype for en behandling.
     * Eks. det kan være ett Innvilgelsesbrev og ett Forvaltningsbrev tilhørende samme behandlingId
     * Men ikke 2 Innvilgelsesbrev for samme behandlingId
     */
    fun hentNyesteAktiveBestilling(behandlingId: BehandlingId, typeBrev: TypeBrev): Brevbestilling? {
        return brevbestillingRepository.hent(behandlingId)
            .filter { it.typeBrev == typeBrev }
            .filter { !it.status.erTilbakestilt() }
            .maxByOrNull { it.opprettet }
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

    fun hentBrevbestilling(brevbestillingReferanse: UUID): Brevbestilling {
        return brevbestillingRepository.hent(BrevbestillingReferanse(brevbestillingReferanse))
    }

    fun hentBrevbestillinger(behandlingReferanse: BehandlingReferanse): List<Brevbestilling> {
        val behandling = behandlingRepository.hent(behandlingReferanse)
        return brevbestillingRepository.hent(behandling.id)
    }

    fun hentBrevbestillinger(behandlingId: BehandlingId): List<Brevbestilling> {
        return brevbestillingRepository.hent(behandlingId)
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
        val brevbestilling = brevbestillingRepository.hent(referanse)
        if (!brevbestilling.status.kanGjenopptas()) {
            throw IllegalStateException("Gjenoppta bestilling er ikke mulig for brevbestillinger med status ${brevbestilling.status}.")
        }
        brevbestillingGateway.gjenoppta(referanse)
        brevbestillingRepository.oppdaterStatus(
            behandlingId = behandlingId,
            referanse = referanse,
            status = Status.FORHÅNDSVISNING_KLAR
        )
    }

    /**
     * Tilbakestill brevbestillinger benyttes av avklaringsbehovService
     *      - sletter bestillingen fullstendig fra db-tabellen i aap-brev
     *      - beholder bestillingen med status TILBAKESTILT i db-tabellen i aap-behandlingsflyt
     */
    private fun tilbakestill(behandlingId: BehandlingId, brevbestillingReferanse: UUID) {
        val referanse = BrevbestillingReferanse(brevbestillingReferanse)
        brevbestillingGateway.slett(referanse)
        brevbestillingRepository.oppdaterStatus(
            behandlingId = behandlingId,
            referanse = referanse,
            status = Status.TILBAKESTILT
        )
    }

    /**
     * Annuller brevbestillinger benyttes for å slette en bestilling fullstendig fra db-tabellen i aap-brev
     */
    private fun annuller(behandlingId: BehandlingId, referanse: BrevbestillingReferanse) {
        brevbestillingGateway.slett(referanse)
        brevbestillingRepository.oppdaterStatus(
            behandlingId = behandlingId,
            referanse = referanse,
            status = Status.ANNULLERT
        )
    }

    fun tilbakestillAlleAktiveBestillingerOmVedtakbrev(behandlingId: BehandlingId) {
        hentBrevbestillinger(behandlingId)
            .filter { it.typeBrev.erVedtak() }
            .filter { it.status == Status.FORHÅNDSVISNING_KLAR }
            .forEach {
                tilbakestill(behandlingId, it.referanse.brevbestillingReferanse)
            }
    }

}
