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
    fun `tilbakestillVedtakBrevBestillinger hverken gjenopptar eller avbryter hvis ingen brevbestillinger finnes`() {

        brevbestillingService.tilbakestillVedtakBrevBestillinger(behandlingId)

        verify(exactly = 0) { brevbestillingGateway.gjenoppta(any()) }
        verify(exactly = 0) { brevbestillingGateway.avbryt(any()) }
    }

    @Test
    fun `tilbakestillVedtakBrevBestillinger hverken gjenopptar eller avbryter hvis ingen brev av typen vedtak finnes`() {
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = TypeBrev.FORVALTNINGSMELDING,
            bestillingReferanse = BrevbestillingReferanse(UUID.randomUUID()),
            status = Status.FORHÅNDSVISNING_KLAR
        )

        brevbestillingService.tilbakestillVedtakBrevBestillinger(behandlingId)

        verify(exactly = 0) { brevbestillingGateway.gjenoppta(any()) }
        verify(exactly = 0) { brevbestillingGateway.avbryt(any()) }
    }

    @Test
    fun `tilbakestillVedtakBrevBestillinger hverken gjenopptar eller avbryter hvis alle vedtaksbrev har status FULLFØRT`() {
        val brevbestillinger = opprettNyBrevbestillingForHverBrevTypeIRepo()
        for (brevbestilling in brevbestillinger.filter { it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevbestilling.referanse, Status.FULLFØRT)
        }

        brevbestillingService.tilbakestillVedtakBrevBestillinger(behandlingId)

        verify(exactly = 0) { brevbestillingGateway.gjenoppta(any()) }
        verify(exactly = 0) { brevbestillingGateway.avbryt(any()) }
    }

    @Test
    fun `tilbakestillVedtakBrevBestillinger avbryter alle vedtaksbrev med status FORHÅNDSVISNING_KLAR`() {
        val brevbestillinger = opprettNyBrevbestillingForHverBrevTypeIRepo()
        for (brevbestilling in brevbestillinger.filter { it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevbestilling.referanse, Status.FORHÅNDSVISNING_KLAR)
        }

        brevbestillingService.tilbakestillVedtakBrevBestillinger(behandlingId)

        val antallVedtakBrev = TypeBrev.entries.filter { it.erVedtak() }.size
        verify(exactly = antallVedtakBrev) { brevbestillingGateway.avbryt(any()) }
        brevbestillingService.hentBrevbestillinger(behandlingId)
            .filter { it.typeBrev.erVedtak() }
            .forEach { assertEquals(Status.AVBRUTT, it.status) }
        brevbestillingService.hentBrevbestillinger(behandlingId)
            .filter { !it.typeBrev.erVedtak() }
            .forEach { assertEquals(Status.FORHÅNDSVISNING_KLAR, it.status) }
    }

    @Test
    fun `tilbakestillVedtakBrevBestillinger endrer ikke vedtaksbrev med status AVBRUTT`() {
        val brevbestillinger = opprettNyBrevbestillingForHverBrevTypeIRepo()
        for (brevbestilling in brevbestillinger.filter { it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevbestilling.referanse, Status.AVBRUTT)
        }
        brevbestillingService.tilbakestillVedtakBrevBestillinger(behandlingId)

        verify(exactly = 0) { brevbestillingGateway.gjenoppta(any()) }
        brevbestillingService.hentBrevbestillinger(behandlingId)
            .filter { it.typeBrev.erVedtak() }
            .forEach { assertEquals(Status.AVBRUTT, it.status) }
    }

    @Test
    fun `tilbakestillVedtakBrevBestillinger avbryter kun vedtaksbrev med status AVBRUTT eller FORHÅNDSVISNING_KLAR`() {
        val nyReferanse = BrevbestillingReferanse(UUID.randomUUID())
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = TypeBrev.VEDTAK_11_18,
            bestillingReferanse = nyReferanse,
            status = Status.FORHÅNDSVISNING_KLAR
        )
        val avbruttReferanse = BrevbestillingReferanse(UUID.randomUUID())
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = TypeBrev.VEDTAK_11_7,
            bestillingReferanse = avbruttReferanse,
            status = Status.AVBRUTT
        )
        val fullførtReferanse = BrevbestillingReferanse(UUID.randomUUID())
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = TypeBrev.VEDTAK_11_9,
            bestillingReferanse = fullførtReferanse,
            status = Status.FULLFØRT
        )

        brevbestillingService.tilbakestillVedtakBrevBestillinger(behandlingId)

        verify(exactly = 1) { brevbestillingGateway.avbryt(any()) }

        val brevbestillinger = brevbestillingService.hentBrevbestillinger(behandlingId)
        assertEquals(Status.AVBRUTT, brevbestillinger.first { it.referanse == nyReferanse }.status)
        assertEquals(Status.AVBRUTT, brevbestillinger.first { it.referanse == avbruttReferanse }.status)
        assertEquals(Status.FULLFØRT, brevbestillinger.first { it.referanse == fullførtReferanse}.status)
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