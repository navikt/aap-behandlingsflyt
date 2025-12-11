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

    /**
     * Brevbestillinger som ikke har ferdigstillAutomatisk lik True vil kunne tilbakestilles mellom status
     * AVBRUTT og FORHÅNDSVISNING_KLAR gitt et framtidig scenario hvor selve BrevSteg kan tilbakestilles.
     *
     * Brevbestillinger med status FULLFØRT kan ikke tilbakestilles.
     *
     * Tilstand SENDT benyttes ikke lenger og start-status for vedtaksbrev i BrevSteg er FORHÅNDSVISNING_KLAR.
     *
     * Normalt vil en brevberstilling ha en endring av status-tilstand som går noe slik
     *   Ny brevbestilling : null -> FORHÅNDSVISNING_KLAR
     *   Avbryt brevbestilling: FORHÅNDSVISNING_KLAR -> AVBRUTT
     *   Gjenoppta brevbestilling : AVBRUTT -> FORHÅNDSVISNING_KLAR
     *   Evt. flere repetisjoner av avbryt og gjenoppta
     *
     *  Tilbakestill medføre en av to status-endringer for brevbestillingene:
     *   1. Nye/gjenopptatt bestilling avbrytes og får tilstand AVBRUTT
     *   2. Avbrutt bestilling forblir i tilstand AVBRUTT
     */
    fun tilbakestillVedtakBrevBestillinger(behandlingId: BehandlingId) {
        val bestillinger = brevbestillingRepository.hent(behandlingId).filter { it.typeBrev.erVedtak() }
        for (bestilling in bestillinger) {
            if (bestilling.status == Status.FORHÅNDSVISNING_KLAR) {
                avbryt(behandlingId, bestilling.referanse)
            }
        }
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
}
