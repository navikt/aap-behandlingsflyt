package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.brev.SignaturService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBrevbestillingRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.random.Random

class BrevbestillingServiceTest {

    val signaturService = mockk<SignaturService>()
    val brevbestillingGateway = mockk<BrevbestillingGateway>()
    val behandlingRepository = mockk<BehandlingRepository>()
    val sakRepository = mockk<SakRepository>()
    val behandlingId = BehandlingId(Random.nextLong())

    @BeforeEach
    fun setUp() {
        every { brevbestillingGateway.gjenoppta(any()) } returns Unit
        InMemoryBrevbestillingRepository.clearMemory()
        // Populer BrevbestillingRepo med ett bestillings-innslag for hver brevtype med start-status FORHÅNDSVISNING_KLAR
        val ikkeEndeTilstand = Status.FORHÅNDSVISNING_KLAR
        for (typeBrev in TypeBrev.entries) {
            InMemoryBrevbestillingRepository.lagre(
                behandlingId = behandlingId,
                typeBrev = typeBrev,
                bestillingReferanse = BrevbestillingReferanse(UUID.randomUUID()),
                status = ikkeEndeTilstand
            )
        }
    }

    @Test
    fun `hentTilbakestillbareBestillingerOmVedtak returnerer emptyList hvis ingen brevbestillinger finnes`() {
        InMemoryBrevbestillingRepository.clearMemory()
        val brevbestillingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )

        val resultat = brevbestillingService.hentTilbakestillbareBestillingerOmVedtak(behandlingId)

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `hentTilbakestillbareBestillingerOmVedtak returnerer emptyList hvis alle vedtaksbrev har endestatus`() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.FULLFØRT)
        }

        val resultat = brevbestllingService.hentTilbakestillbareBestillingerOmVedtak(behandlingId)

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `hentTilbakestillbareBestillingerOmVedtak returnerer emptyList hvis ingen vedtaksbrev finnes`() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )
        InMemoryBrevbestillingRepository.clearMemory()
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = TypeBrev.FORVALTNINGSMELDING,
            bestillingReferanse = BrevbestillingReferanse(UUID.randomUUID()),
            status = Status.FORHÅNDSVISNING_KLAR
        )

        val resultat = brevbestllingService.hentTilbakestillbareBestillingerOmVedtak(behandlingId)

        assertThat(TypeBrev.FORVALTNINGSMELDING.erVedtak()).isFalse
        assertThat(resultat).isEmpty()
    }

    @Test
    fun `hentTilbakestillbareBestillingerOmVedtak returnerer korrekt liste når alle vedtaksbrev har status ForhåndsvisningKlar`() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )

        val resultat = brevbestllingService.hentTilbakestillbareBestillingerOmVedtak(behandlingId)

        val antallVedtakBrev = TypeBrev.entries.filter { it.erVedtak() }.size
        assertThat(resultat).hasSize(antallVedtakBrev)
        for (brevBestilling in resultat) {
            assertThat(brevBestilling.status).isEqualTo(Status.FORHÅNDSVISNING_KLAR)
            assertThat(brevBestilling.typeBrev.erVedtak()).isTrue
        }
    }

    @Test
    fun `hentTilbakestillbareBestillingerOmVedtak() returnerer korrekt liste kun med vedtaksBrev som har status FORHÅNDSVISNING_KLAR og SENDT`() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )
        val vedtakBrevBestillinger =
            InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId).filter { it.typeBrev.erVedtak() }
        val fullførtBrevBestilling = vedtakBrevBestillinger[0]
        InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, fullførtBrevBestilling.referanse, Status.FULLFØRT)
        val avbruttBrevBestilling = vedtakBrevBestillinger[1]
        InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, avbruttBrevBestilling.referanse, Status.AVBRUTT)

        val resultat = brevbestllingService.hentTilbakestillbareBestillingerOmVedtak(behandlingId)

        val antallTilbakestillbareVedtakBrev = TypeBrev.entries.filter { it.erVedtak() }.size - 2
        assertThat(resultat).hasSize(antallTilbakestillbareVedtakBrev)
        for (brevBestilling in resultat) {
            assertThat(brevBestilling.referanse).isNotEqualTo(fullførtBrevBestilling.referanse)
            assertThat(brevBestilling.referanse).isNotEqualTo(avbruttBrevBestilling.referanse)
            assertThat(brevBestilling.status).isEqualTo(Status.FORHÅNDSVISNING_KLAR)
            assertThat(brevBestilling.typeBrev.erVedtak()).isTrue
        }
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand returnerer true hvis ingen brevbestillinger finnes`() {
        InMemoryBrevbestillingRepository.clearMemory()
        val brevbestillingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )

        val resultat = brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertThat(resultat).isTrue
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand returnerer false hvis ingen vedtaksbrev har ende-tilstand`() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )

        val resultat = brevbestllingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertThat(resultat).isFalse
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisIkkeVedtakBrevHarEndeTilstandMenAndreHarFullført`() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { !it.typeBrev.erVedtak() }) {
            val referanse = brevBestilling.referanse
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, referanse, Status.FULLFØRT)
        }

        val resultat = brevbestllingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisIkkeVedtakBrevHarEndeTilstandMenAndreHarSendt`() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { !it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.SENDT)
        }

        val resultat = brevbestllingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisIkkeVedtakBrevHarEndeTilstandMenAndreHarAvbrutt`() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { !it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.SENDT)
        }

        val resultat = brevbestllingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisEttVedtakBrevHarEndeTilstandFullført`() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        val brevBestilling = brevBestillinger.first { it.typeBrev.erVedtak() }
        InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.FULLFØRT)

        val resultat = brevbestllingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisEttVedtakBrevHarEndeTilstandSent`() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        val brevBestilling = brevBestillinger.first { it.typeBrev.erVedtak() }
        InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.SENDT)

        val resultat = brevbestllingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisEttVedtakBrevHarEndeTilstandAvbrutt`() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        val brevBestilling = brevBestillinger.first { it.typeBrev.erVedtak() }
        InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.AVBRUTT)

        val resultat = brevbestllingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnTrue_hvisAlleVedtakBrevHarEndeTilstandFullført`() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.FULLFØRT)
        }

        val resultat = brevbestllingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertTrue(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisAlleVedtakBrevHarEndeTilstandSent`() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.SENDT)
        }

        val resultat = brevbestllingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnTrue_hvisAlleVedtakBrevHarEndeTilstandAvbrutt`() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.AVBRUTT)
        }

        val resultat = brevbestllingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertTrue(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnTrue_hvisIngenVedtakBrevFinnes`() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )
        InMemoryBrevbestillingRepository.clearMemory()
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = TypeBrev.FORVALTNINGSMELDING,
            bestillingReferanse = BrevbestillingReferanse(UUID.randomUUID()),
            status = Status.FORHÅNDSVISNING_KLAR
        )

        val resultat = brevbestllingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(TypeBrev.FORVALTNINGSMELDING.erVedtak())
        assertTrue(resultat)
    }

    @Test
    fun `gjenoppta tidligere avbrutt brevbestilling tilbakestiller status og kaller gjenoppta() i brevGateway mot aap-brev api`() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )
        InMemoryBrevbestillingRepository.clearMemory()
        val referanse = BrevbestillingReferanse(UUID.randomUUID())
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV,
            bestillingReferanse = referanse,
            status = Status.AVBRUTT
        )

        brevbestllingService.gjenopptaBestilling(behandlingId, referanse)

        val resultat = InMemoryBrevbestillingRepository.hent(referanse)
        assertThat(resultat.status).isEqualTo(Status.FORHÅNDSVISNING_KLAR)
        verify { brevbestillingGateway.gjenoppta(referanse)}
    }

}