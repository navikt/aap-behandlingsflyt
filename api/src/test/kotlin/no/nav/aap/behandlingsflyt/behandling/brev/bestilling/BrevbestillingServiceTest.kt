package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.behandling.brev.ForhåndsvarselKlageFormkrav
import no.nav.aap.behandlingsflyt.behandling.brev.SignaturService
import no.nav.aap.behandlingsflyt.behandling.brev.VedtakEndring
import no.nav.aap.behandlingsflyt.flyt.testutil.FakeBrevbestillingGateway
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.FakeOppgavestyringGateway
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBrevbestillingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.minimalGatewayProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.random.Random

class BrevbestillingServiceTest {
    @Test
    fun `bestiller og lagrer brevbestilling`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()

        val brevbestillingReferanse = brevbestillingService().bestill(
            behandlingId = behandling.id,
            brevBehov = VedtakEndring,
            unikReferanse = UUID.randomUUID().toString(),
            ferdigstillAutomatisk = false,
            vedlegg = null,
            brukApiV3 = false
        )

        assertThat(InMemoryBrevbestillingRepository.hent(BrevbestillingReferanse(brevbestillingReferanse))).isNotNull
    }

    @Test
    fun `returnerer samme bestilling dersom man gjør samme bestilling flere ganger`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val unikReferanse = UUID.randomUUID().toString()

        val brevbestillingGateway = FakeBrevbestillingGateway()
        val brevbestillingService = brevbestillingService(fakeBrevbestillingGateway = brevbestillingGateway)

        val bestilling1 = brevbestillingService.bestill(
            behandlingId = behandling.id,
            brevBehov = VedtakEndring,
            unikReferanse = unikReferanse,
            ferdigstillAutomatisk = false,
            vedlegg = null,
            brukApiV3 = false
        )

        val brevbestillingReferanse = brevbestillingGateway.hent(BrevbestillingReferanse(bestilling1)).referanse

        assertThat(bestilling1).isEqualTo(brevbestillingReferanse)

        val bestilling2 = brevbestillingService.bestill(
            behandlingId = behandling.id,
            brevBehov = VedtakEndring,
            unikReferanse = unikReferanse,
            ferdigstillAutomatisk = false,
            vedlegg = null,
            brukApiV3 = false
        )

        assertThat(bestilling2).isEqualTo(brevbestillingReferanse)
    }

    @Test
    fun `holder brevbestillinger adskilt når en tidligere bestilling gjenbrukes`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val gateway = FakeBrevbestillingGateway()
        val service = brevbestillingService(gateway)
        val unikReferanse = UUID.randomUUID().toString()

        fun bestill(referanse: String) = BrevbestillingReferanse(
            service.bestill(
                behandlingId = behandling.id,
                brevBehov = VedtakEndring,
                unikReferanse = referanse,
                ferdigstillAutomatisk = false
            )
        )

        val første = bestill(unikReferanse)
        val andre = bestill(UUID.randomUUID().toString())
        val gjentatt = bestill(unikReferanse)

        assertThat(gjentatt).isEqualTo(første).isNotEqualTo(andre)
        assertThat(gateway.hent(gjentatt).referanse).isEqualTo(første.brevbestillingReferanse)

        service.avbryt(behandling.id, gjentatt)
        assertThat(gateway.hent(første).status).isEqualTo(no.nav.aap.brev.kontrakt.Status.AVBRUTT)
        assertThat(gateway.hent(andre).status).isEqualTo(no.nav.aap.brev.kontrakt.Status.UNDER_ARBEID)

        service.avbryt(behandling.id, andre)
        service.gjenopptaBestilling(behandling.id, første)
        assertThat(gateway.hent(første).status).isEqualTo(no.nav.aap.brev.kontrakt.Status.UNDER_ARBEID)
        assertThat(gateway.hent(andre).status).isEqualTo(no.nav.aap.brev.kontrakt.Status.AVBRUTT)
    }

    @Test
    fun `hentTilbakestillbareBestillingerOmVedtak returnerer emptyList hvis ingen brevbestillinger finnes`() {
        val behandlingId = BehandlingId(Random.nextLong())

        val resultat = brevbestillingService().hentTilbakestillbareBestillingerOmVedtak(behandlingId)

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `hentTilbakestillbareBestillingerOmVedtak returnerer emptyList hvis alle vedtaksbrev har endestatus`() {
        val behandlingId = BehandlingId(Random.nextLong())
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.FULLFØRT)
        }

        val resultat = brevbestillingService().hentTilbakestillbareBestillingerOmVedtak(behandlingId)

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `hentTilbakestillbareBestillingerOmVedtak returnerer emptyList hvis ingen vedtaksbrev finnes`() {
        val behandlingId = BehandlingId(Random.nextLong())
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = TypeBrev.FORVALTNINGSMELDING,
            bestillingReferanse = BrevbestillingReferanse(UUID.randomUUID()),
            status = Status.FORHÅNDSVISNING_KLAR
        )

        val resultat = brevbestillingService().hentTilbakestillbareBestillingerOmVedtak(behandlingId)

        assertThat(TypeBrev.FORVALTNINGSMELDING.erVedtak()).isFalse
        assertThat(resultat).isEmpty()
    }

    @Test
    fun `hentTilbakestillbareBestillingerOmVedtak returnerer korrekt liste når alle vedtaksbrev har status ForhåndsvisningKlar`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)

        val resultat = brevbestillingService().hentTilbakestillbareBestillingerOmVedtak(behandlingId)

        val antallVedtakBrev = TypeBrev.entries.filter { it.erVedtak() }.size
        assertThat(resultat).hasSize(antallVedtakBrev)
        for (brevBestilling in resultat) {
            assertThat(brevBestilling.status).isEqualTo(Status.FORHÅNDSVISNING_KLAR)
            assertThat(brevBestilling.typeBrev.erVedtak()).isTrue
        }
    }

    @Test
    fun `hentTilbakestillbareBestillingerOmVedtak() returnerer korrekt liste kun med vedtaksBrev som har status FORHÅNDSVISNING_KLAR og SENDT`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)

        val vedtakBrevBestillinger =
            InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId).filter { it.typeBrev.erVedtak() }
        val fullførtBrevBestilling = vedtakBrevBestillinger[0]
        InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, fullførtBrevBestilling.referanse, Status.FULLFØRT)
        val avbruttBrevBestilling = vedtakBrevBestillinger[1]
        InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, avbruttBrevBestilling.referanse, Status.AVBRUTT)

        val resultat = brevbestillingService().hentTilbakestillbareBestillingerOmVedtak(behandlingId)

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
        val behandlingId = BehandlingId(Random.nextLong())

        val resultat = brevbestillingService().erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertThat(resultat).isTrue
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand returnerer false hvis ingen vedtaksbrev har ende-tilstand`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)

        val resultat = brevbestillingService().erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertThat(resultat).isFalse
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand returnerer false hvis ikke vedtakbrev har endetilstand men andre har fullført`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { !it.typeBrev.erVedtak() }) {
            val referanse = brevBestilling.referanse
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, referanse, Status.FULLFØRT)
        }

        val resultat = brevbestillingService().erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand returnerer false hvis ikke vedtakbrev har endetilstand men andre har sendt`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { !it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.SENDT)
        }

        val resultat = brevbestillingService().erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand returnerer false hvis ikke vedtakbrev har endetilstand men andre har avbrutt`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { !it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.SENDT)
        }

        val resultat = brevbestillingService().erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand returnerer false hvis ett vedtakbrev har endetilstand fullført`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        val brevBestilling = brevBestillinger.first { it.typeBrev.erVedtak() }
        InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.FULLFØRT)

        val resultat = brevbestillingService().erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand returnerer false hvis ett vedtakbrev har endetilstand sendt`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        val brevBestilling = brevBestillinger.first { it.typeBrev.erVedtak() }
        InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.SENDT)

        val resultat = brevbestillingService().erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand returnerer false hvis ett vedtakbrev har endetilstand avbrutt`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        val brevBestilling = brevBestillinger.first { it.typeBrev.erVedtak() }
        InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.AVBRUTT)

        val resultat = brevbestillingService().erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand returnerer true hvis alle vedtakbrev har endetilstand fullført`() {
        val behandlingId = BehandlingId(Random.nextLong())
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.FULLFØRT)
        }

        val resultat = brevbestillingService().erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertTrue(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand returnerer false hvis alle vedtakbrev har endetilstand sendt`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.SENDT)
        }

        val resultat = brevbestillingService().erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand returnerer true hvis alle vedtakbrev har endetilstand avbrutt`() {
        val behandlingId = BehandlingId(Random.nextLong())
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.AVBRUTT)
        }

        val resultat = brevbestillingService().erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertTrue(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand returnerer true hvis ingen vedtaksbrev finnes`() {
        val behandlingId = BehandlingId(Random.nextLong())
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = TypeBrev.FORVALTNINGSMELDING,
            bestillingReferanse = BrevbestillingReferanse(UUID.randomUUID()),
            status = Status.FORHÅNDSVISNING_KLAR
        )

        val resultat = brevbestillingService().erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(TypeBrev.FORVALTNINGSMELDING.erVedtak())
        assertTrue(resultat)
    }

    @Test
    fun `gjenoppta tidligere avbrutt brevbestilling tilbakestiller status og kaller gjenoppta() i brevGateway mot aap-brev api`() {
        val (_, behandling) = opprettInMemorySakOgBehandling()
        val fakeBrevbestillingGateway = FakeBrevbestillingGateway()
        val brevbestillingService = brevbestillingService(fakeBrevbestillingGateway)
        val referanse = BrevbestillingReferanse(
            brevbestillingService.bestill(
                behandlingId = behandling.id,
                brevBehov = ForhåndsvarselKlageFormkrav,
                unikReferanse = UUID.randomUUID().toString(),
                ferdigstillAutomatisk = false
            )
        )
        brevbestillingService.avbryt(behandling.id, referanse)

        brevbestillingService.gjenopptaBestilling(behandling.id, referanse)

        val resultat = InMemoryBrevbestillingRepository.hent(referanse)
        assertThat(resultat?.status).isEqualTo(Status.FORHÅNDSVISNING_KLAR)

        val brevbestilling = fakeBrevbestillingGateway.hent(referanse)
        assertThat(brevbestilling.referanse).isEqualTo(referanse.brevbestillingReferanse)
        assertThat(brevbestilling.status).isEqualTo(no.nav.aap.brev.kontrakt.Status.UNDER_ARBEID)
    }

    private fun lagreBrevbestillingerMedStatus(
        behandlingId: BehandlingId,
        typerBrev: List<TypeBrev>,
        status: Status
    ) {
        typerBrev.forEach { typeBrev ->
            InMemoryBrevbestillingRepository.lagre(
                behandlingId = behandlingId,
                typeBrev = typeBrev,
                bestillingReferanse = BrevbestillingReferanse(UUID.randomUUID()),
                status = status
            )
        }
    }

    private fun brevbestillingService(fakeBrevbestillingGateway: FakeBrevbestillingGateway = FakeBrevbestillingGateway()): BrevbestillingService {
        val brevbestillingGateway: BrevbestillingGateway = fakeBrevbestillingGateway
        return BrevbestillingService(
            signaturService = SignaturService(
                inMemoryRepositoryProvider,
                minimalGatewayProvider { register<FakeOppgavestyringGateway>() }),
            brevbestillingGateway = brevbestillingGateway,
            brevbestillingRepository = InMemoryBrevbestillingRepository,
            behandlingRepository = InMemoryBehandlingRepository,
            sakRepository = InMemorySakRepository
        )
    }
}
