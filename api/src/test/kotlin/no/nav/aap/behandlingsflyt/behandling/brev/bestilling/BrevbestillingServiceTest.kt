package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.brev.SignaturService
import no.nav.aap.behandlingsflyt.behandling.brev.VedtakEndring
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBrevbestillingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.*
import kotlin.random.Random

class BrevbestillingServiceTest {

    val signaturService = mockk<SignaturService>()
    val brevbestillingGateway = mockk<BrevbestillingGateway>()
    val brevbestillingService = BrevbestillingService(
        signaturService = signaturService,
        brevbestillingGateway = brevbestillingGateway,
        brevbestillingRepository = InMemoryBrevbestillingRepository,
        behandlingRepository = InMemoryBehandlingRepository,
        sakRepository = InMemorySakRepository
    )

    @Test
    fun `bestiller og lagrer brevbestilling`() {
        val behandling = opprettSakOgBehandling()
        every {
            brevbestillingGateway.bestillBrev(
                saksnummer = any(),
                brukerIdent = any(),
                behandlingReferanse = behandling.referanse,
                unikReferanse = any(),
                brevBehov = any(),
                vedlegg = anyNullable(),
                ferdigstillAutomatisk = any(),
                brukApiV3 = any()
            )
        } returns BrevbestillingReferanse(UUID.randomUUID())
        val brevbestillingReferanse = brevbestillingService.bestill(
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
    fun `bestill feiler dersom man bestiller samme brev to ganger`() {
        val behandling = opprettSakOgBehandling()
        val unikReferanse = UUID.randomUUID().toString()
        val brevbestillingReferanse = BrevbestillingReferanse(UUID.randomUUID())

        every {
            brevbestillingGateway.bestillBrev(
                saksnummer = any(),
                brukerIdent = any(),
                behandlingReferanse = behandling.referanse,
                unikReferanse = unikReferanse,
                brevBehov = any(),
                vedlegg = anyNullable(),
                ferdigstillAutomatisk = any(),
                brukApiV3 = any()
            )
        } returns brevbestillingReferanse

        brevbestillingService.bestill(
            behandlingId = behandling.id,
            brevBehov = VedtakEndring,
            unikReferanse = unikReferanse,
            ferdigstillAutomatisk = false,
            vedlegg = null,
            brukApiV3 = false
        )

        assertThrows<IllegalStateException> {
            brevbestillingService.bestill(
                behandlingId = behandling.id,
                brevBehov = VedtakEndring,
                unikReferanse = unikReferanse,
                ferdigstillAutomatisk = false,
                vedlegg = null,
                brukApiV3 = false
            )
        }
    }

    @Test
    fun `hentTilbakestillbareBestillingerOmVedtak returnerer emptyList hvis ingen brevbestillinger finnes`() {
        val behandlingId = BehandlingId(Random.nextLong())

        val resultat = brevbestillingService.hentTilbakestillbareBestillingerOmVedtak(behandlingId)

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `hentTilbakestillbareBestillingerOmVedtak returnerer emptyList hvis alle vedtaksbrev har endestatus`() {
        val behandlingId = BehandlingId(Random.nextLong())
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.FULLFØRT)
        }

        val resultat = brevbestillingService.hentTilbakestillbareBestillingerOmVedtak(behandlingId)

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

        val resultat = brevbestillingService.hentTilbakestillbareBestillingerOmVedtak(behandlingId)

        assertThat(TypeBrev.FORVALTNINGSMELDING.erVedtak()).isFalse
        assertThat(resultat).isEmpty()
    }

    @Test
    fun `hentTilbakestillbareBestillingerOmVedtak returnerer korrekt liste når alle vedtaksbrev har status ForhåndsvisningKlar`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)

        val resultat = brevbestillingService.hentTilbakestillbareBestillingerOmVedtak(behandlingId)

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

        val resultat = brevbestillingService.hentTilbakestillbareBestillingerOmVedtak(behandlingId)

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

        val resultat = brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertThat(resultat).isTrue
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand returnerer false hvis ingen vedtaksbrev har ende-tilstand`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)

        val resultat = brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertThat(resultat).isFalse
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisIkkeVedtakBrevHarEndeTilstandMenAndreHarFullført`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { !it.typeBrev.erVedtak() }) {
            val referanse = brevBestilling.referanse
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, referanse, Status.FULLFØRT)
        }

        val resultat = brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisIkkeVedtakBrevHarEndeTilstandMenAndreHarSendt`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { !it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.SENDT)
        }

        val resultat = brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisIkkeVedtakBrevHarEndeTilstandMenAndreHarAvbrutt`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { !it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.SENDT)
        }

        val resultat = brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisEttVedtakBrevHarEndeTilstandFullført`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        val brevBestilling = brevBestillinger.first { it.typeBrev.erVedtak() }
        InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.FULLFØRT)

        val resultat = brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisEttVedtakBrevHarEndeTilstandSent`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        val brevBestilling = brevBestillinger.first { it.typeBrev.erVedtak() }
        InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.SENDT)

        val resultat = brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisEttVedtakBrevHarEndeTilstandAvbrutt`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        val brevBestilling = brevBestillinger.first { it.typeBrev.erVedtak() }
        InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.AVBRUTT)

        val resultat = brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnTrue_hvisAlleVedtakBrevHarEndeTilstandFullført`() {
        val behandlingId = BehandlingId(Random.nextLong())
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.FULLFØRT)
        }

        val resultat = brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertTrue(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnFalse_hvisAlleVedtakBrevHarEndeTilstandSent`() {
        val behandlingId = BehandlingId(Random.nextLong())
        lagreBrevbestillingerMedStatus(behandlingId, TypeBrev.entries, Status.FORHÅNDSVISNING_KLAR)
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.SENDT)
        }

        val resultat = brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnTrue_hvisAlleVedtakBrevHarEndeTilstandAvbrutt`() {
        val behandlingId = BehandlingId(Random.nextLong())
        val brevBestillinger = InMemoryBrevbestillingRepository.hent(behandlingId = behandlingId)
        for (brevBestilling in brevBestillinger.filter { it.typeBrev.erVedtak() }) {
            InMemoryBrevbestillingRepository.oppdaterStatus(behandlingId, brevBestilling.referanse, Status.AVBRUTT)
        }

        val resultat = brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertTrue(resultat)
    }

    @Test
    fun `erAlleBestillingerOmVedtakIEndeTilstand_returnTrue_hvisIngenVedtakBrevFinnes`() {
        val behandlingId = BehandlingId(Random.nextLong())
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = TypeBrev.FORVALTNINGSMELDING,
            bestillingReferanse = BrevbestillingReferanse(UUID.randomUUID()),
            status = Status.FORHÅNDSVISNING_KLAR
        )

        val resultat = brevbestillingService.erAlleBestillingerOmVedtakIEndeTilstand(behandlingId)

        assertFalse(TypeBrev.FORVALTNINGSMELDING.erVedtak())
        assertTrue(resultat)
    }

    @Test
    fun `gjenoppta tidligere avbrutt brevbestilling tilbakestiller status og kaller gjenoppta() i brevGateway mot aap-brev api`() {
        every { brevbestillingGateway.gjenoppta(any()) } returns Unit
        val behandlingId = BehandlingId(Random.nextLong())
        val referanse = BrevbestillingReferanse(UUID.randomUUID())
        InMemoryBrevbestillingRepository.lagre(
            behandlingId = behandlingId,
            typeBrev = TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV,
            bestillingReferanse = referanse,
            status = Status.AVBRUTT
        )

        brevbestillingService.gjenopptaBestilling(behandlingId, referanse)

        val resultat = InMemoryBrevbestillingRepository.hent(referanse)
        assertThat(resultat?.status).isEqualTo(Status.FORHÅNDSVISNING_KLAR)
        verify { brevbestillingGateway.gjenoppta(referanse) }
    }

    private fun opprettSakOgBehandling(): Behandling {
        val person = Person(PersonId(1), UUID.randomUUID(), listOf(genererIdent(LocalDate.now().minusYears(23))))
        val sak = InMemorySakRepository.finnEllerOpprett(person, Periode(LocalDate.now(), LocalDate.now().plusYears(1)))
        return InMemoryBehandlingRepository.opprettBehandling(
            sak.id,
            Førstegangsbehandling,
            null,
            VurderingsbehovOgÅrsak(
                listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                ÅrsakTilOpprettelse.SØKNAD
            )
        )
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
}
