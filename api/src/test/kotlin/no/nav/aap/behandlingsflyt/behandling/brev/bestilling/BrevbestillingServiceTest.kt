package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.brev.SignaturService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBrevbestillingRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.random.Random
import kotlin.test.assertEquals

class BrevbestillingServiceTest {

    private lateinit var brevbestillingService: BrevbestillingService
    private val signaturService = mockk<SignaturService>()
    private val brevbestillingGateway = mockk<BrevbestillingGateway>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val sakRepository = mockk<SakRepository>()
    private val behandlingId = BehandlingId(Random.nextLong())

    @BeforeEach
    fun setUp() {
        every { brevbestillingGateway.gjenoppta(any()) } returns Unit
        every { brevbestillingGateway.avbryt(any()) } returns Unit
        every { brevbestillingGateway.slett(any()) } returns Unit
        InMemoryBrevbestillingRepository.clearMemory()
        brevbestillingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )
    }

    @Test
    fun `tilbakestillBrevBestilling sletter og endrer ikke brevbestilling som ikke er av typen vedtak`() {
        val referanse = BrevbestillingReferanse(UUID.randomUUID())
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = TypeBrev.FORVALTNINGSMELDING,
            bestillingReferanse = referanse,
            status = Status.FORHÅNDSVISNING_KLAR
        )

        brevbestillingService.tilbakestillAlleAktiveBestillingerOmVedtakbrev(behandlingId)

        verify(exactly = 0) { brevbestillingGateway.slett(any()) }
        val bestilling = brevbestillingService.hentBrevbestilling(referanse.brevbestillingReferanse)
        assertEquals(TypeBrev.FORVALTNINGSMELDING, bestilling.typeBrev)
        assertEquals(Status.FORHÅNDSVISNING_KLAR, bestilling.status)
    }

    @Test
    fun `tilbakestillBrevBestilling sletter ikke brevbestilling med status FULLFØRT`() {
        val referanse = BrevbestillingReferanse(UUID.randomUUID())
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = TypeBrev.VEDTAK_INNVILGELSE,
            bestillingReferanse = referanse,
            status = Status.FULLFØRT
        )

        brevbestillingService.tilbakestillAlleAktiveBestillingerOmVedtakbrev(behandlingId)

        verify(exactly = 0) { brevbestillingGateway.slett(any()) }
        val bestilling = brevbestillingService.hentBrevbestilling(referanse.brevbestillingReferanse)
        assertEquals(TypeBrev.VEDTAK_INNVILGELSE, bestilling.typeBrev)
        assertEquals(Status.FULLFØRT, bestilling.status)
    }

    @Test
    fun `tilbakestillBrevBestilling sletter og endrer status på vedtaksbrev med status FORHÅNDSVISNING_KLAR`() {
        val referanse = BrevbestillingReferanse(UUID.randomUUID())
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = TypeBrev.VEDTAK_INNVILGELSE,
            bestillingReferanse = referanse,
            status = Status.FORHÅNDSVISNING_KLAR
        )

        brevbestillingService.tilbakestillAlleAktiveBestillingerOmVedtakbrev(behandlingId)

        verify(exactly = 1) { brevbestillingGateway.slett(any()) }
        val bestilling = brevbestillingService.hentBrevbestilling(referanse.brevbestillingReferanse)
        assertEquals(TypeBrev.VEDTAK_INNVILGELSE, bestilling.typeBrev)
        assertEquals(Status.TILBAKESTILT, bestilling.status)
    }

    @Test
    fun `tilbakestillBrevBestilling endrer ikke vedtaksbrev med status AVBRUTT`() {
        val referanse = BrevbestillingReferanse(UUID.randomUUID())
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = TypeBrev.VEDTAK_INNVILGELSE,
            bestillingReferanse = referanse,
            status = Status.AVBRUTT
        )

        brevbestillingService.tilbakestillAlleAktiveBestillingerOmVedtakbrev(behandlingId)

        verify(exactly = 0) { brevbestillingGateway.slett(any()) }
        val bestilling = brevbestillingService.hentBrevbestilling(referanse.brevbestillingReferanse)
        assertEquals(TypeBrev.VEDTAK_INNVILGELSE, bestilling.typeBrev)
        assertEquals(Status.AVBRUTT, bestilling.status)
    }

    @Test
    fun `tilbakestillBrevBestilling sletter kun vedtaksbrev med gitt brevbestillingreferanse`() {
        val nyReferanse = BrevbestillingReferanse(UUID.randomUUID())
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = TypeBrev.VEDTAK_11_18,
            bestillingReferanse = nyReferanse,
            status = Status.FORHÅNDSVISNING_KLAR
        )

        brevbestillingService.tilbakestillAlleAktiveBestillingerOmVedtakbrev(behandlingId)

        verify(exactly = 1) { brevbestillingGateway.slett(any()) }

        val brevbestillinger = brevbestillingService.hentBrevbestillinger(behandlingId)
        assertEquals(Status.TILBAKESTILT, brevbestillinger.first { it.referanse == nyReferanse }.status)
    }

    @Test
    fun `gjenoppta avbrutt brevbestilling endrer status og kaller gjenoppta() i brevbestillingGateway`() {
        val avbruttReferanse = BrevbestillingReferanse(UUID.randomUUID())
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV,
            bestillingReferanse = avbruttReferanse,
            status = Status.AVBRUTT
        )

        brevbestillingService.gjenopptaBestilling(behandlingId, avbruttReferanse)

        val resultat = InMemoryBrevbestillingRepository.hent(avbruttReferanse)
        assertEquals(Status.FORHÅNDSVISNING_KLAR, resultat.status)
        verify { brevbestillingGateway.gjenoppta(avbruttReferanse)}
    }

    private fun opprettNyBrevbestillingForHverBrevTypeIRepo(): List<Brevbestilling> {
        val nyopprettetStatus = Status.FORHÅNDSVISNING_KLAR
        for (typeBrev in TypeBrev.entries) {
            InMemoryBrevbestillingRepository.lagre(
                behandlingId = behandlingId,
                typeBrev = typeBrev,
                bestillingReferanse = BrevbestillingReferanse(UUID.randomUUID()),
                status = nyopprettetStatus
            )
        }
        return InMemoryBrevbestillingRepository.hent(behandlingId)
    }


}