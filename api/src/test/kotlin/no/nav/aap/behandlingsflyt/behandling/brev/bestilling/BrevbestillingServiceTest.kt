package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.brev.SignaturService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBrevbestillingRepository
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BrevbestillingServiceTest {

    val signaturService = mockk<SignaturService>()
    val brevbestillingGateway = mockk<BrevbestillingGateway>()
    val behandlingRepository = mockk<BehandlingRepository>()
    val sakRepository = mockk<SakRepository>()
    val behandlingId = BehandlingId(1L)

    @BeforeEach
    fun setUp() {
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
    fun hentTilbakestillbareBestillingerOmVedtak_returnEmptyList_hvisIngenBrevBestillingerFinnes() {
        InMemoryBrevbestillingRepository.clearMemory()
        val brevbestillingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )

        val resultat = brevbestillingService.hentTilbakestillbareBestillingerOmVedtak(behandlingId)

        assertTrue(resultat.isEmpty())
    }

    @Test
    fun hentTilbakestillbareBestillingerOmVedtak_returnEmptyList_hvisIngenVedtaksBrevHarStatusForhåndsvisningKlar() {
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

        val resultat = brevbestllingService.hentTilbakestillbareBestillingerOmVedtak(behandlingId)

        assertTrue(resultat.isEmpty())
    }

    @Test
    fun hentTilbakestillbareBestillingerOmVedtak_returnEmptyList_hvisIngenVedtakBrevFinnes() {
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

        assertFalse(TypeBrev.FORVALTNINGSMELDING.erVedtak())
        assertTrue(resultat.isEmpty())
    }

    @Test
    fun hentTilbakestillbareBestillingerOmVedtak_returnKorrektListe_nårAlleVedtaksBrevHarStatusForhåndsvisningKlar() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )

        val resultat = brevbestllingService.hentTilbakestillbareBestillingerOmVedtak(behandlingId)

        val antallVedtakBrev = TypeBrev.entries.filter { it.erVedtak() }.size
        assertEquals(antallVedtakBrev, resultat.size)
        for (brevBestilling in resultat) {
            assertEquals(Status.FORHÅNDSVISNING_KLAR, brevBestilling.status)
            assertTrue(brevBestilling.typeBrev.erVedtak())
        }
    }

    @Test
    fun hentTilbakestillbareBestillingerOmVedtak_returnKorrektListe_kunMedVedtaksBrevSomHarStatusForhåndsvisningKlar() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )
        val vedtakBrevBestillinger =
            InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId).filter { it.typeBrev.erVedtak() }
        val sendtBrevBestilling = vedtakBrevBestillinger[0]
        InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, sendtBrevBestilling.referanse, Status.SENDT)
        val fullførtBrevBestilling = vedtakBrevBestillinger[1]
        InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, fullførtBrevBestilling.referanse, Status.FULLFØRT)
        val avbruttBrevBestilling = vedtakBrevBestillinger[2]
        InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, avbruttBrevBestilling.referanse, Status.AVBRUTT)

        val resultat = brevbestllingService.hentTilbakestillbareBestillingerOmVedtak(behandlingId)

        val antallTilbakestillbareVedtakBrev = TypeBrev.entries.filter { it.erVedtak() }.size - 3
        assertEquals(antallTilbakestillbareVedtakBrev, resultat.size)
        for (brevBestilling in resultat) {
            assertNotEquals(sendtBrevBestilling.referanse, brevBestilling.referanse)
            assertNotEquals(fullførtBrevBestilling.referanse, brevBestilling.referanse)
            assertNotEquals(avbruttBrevBestilling.referanse, brevBestilling.referanse)
            assertEquals(Status.FORHÅNDSVISNING_KLAR, brevBestilling.status)
            assertTrue(brevBestilling.typeBrev.erVedtak())
        }
    }

    @Test
    fun erAlleBestillingerOmVedtakIEndeTilstand_returnTrue_hvisIngenBrevBestillingerFinnes() {
        InMemoryBrevbestillingRepository.clearMemory()
        val brevbestillingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )

        val resultat = brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertTrue(resultat)
    }

    @Test
    fun erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisIngenVedtaksBrevHarEndeTilstand() {
        val brevbestllingService = BrevbestillingService(
            signaturService,
            brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository,
            sakRepository
        )

        val resultat = brevbestllingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisIkkeVedtakBrevHarEndeTilstandMenAndreHarFullført() {
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
    fun erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisIkkeVedtakBrevHarEndeTilstandMenAndreHarSendt() {
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
    fun erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisIkkeVedtakBrevHarEndeTilstandMenAndreHarAvbrutt() {
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
    fun erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisEttVedtakBrevHarEndeTilstandFullført() {
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
    fun erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisEttVedtakBrevHarEndeTilstandSent() {
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
    fun erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisEttVedtakBrevHarEndeTilstandAvbrutt() {
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
    fun erAlleBestillingerOmVedtakIEndeTilstand_returnTrue_hvisAlleVedtakBrevHarEndeTilstandFullført() {
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
    fun erAlleBestillingerOmVedtakIEndeTilstand_returnTrue_hvisAlleVedtakBrevHarEndeTilstandSent() {
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

        assertTrue(resultat)
    }

    @Test
    fun erAlleBestillingerOmVedtakIEndeTilstand_returnTrue_hvisAlleVedtakBrevHarEndeTilstandAvbrutt() {
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
    fun erAlleBestillingerOmVedtakIEndeTilstand_returnTrue_hvisIngenVedtakBrevFinnes() {
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

}