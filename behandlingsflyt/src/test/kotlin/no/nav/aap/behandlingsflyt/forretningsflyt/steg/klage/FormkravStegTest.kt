package no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Brevbestilling
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageService
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVarsel
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering
import no.nav.aap.behandlingsflyt.flyt.steg.FantVentebehov
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@ExtendWith(MockKExtension::class)
class FormkravStegTest {
    val trekkKlageServiceMock = mockk<TrekkKlageService>()
    val formkravRepositoryMock = mockk<FormkravRepository>()
    val behandlingRepositoryMock = mockk<BehandlingRepository>()
    val brevbestillingServiceMock = mockk<BrevbestillingService>()
    val avklaringsbehovServiceMock = mockk<AvklaringsbehovService>()
    val avbrytRevurderingServiceMock = mockk<AvbrytRevurderingService>()
    val gatewayProvider = createGatewayProvider {
        register<FakeUnleash>()
    }

    @BeforeEach
    fun setup() {
        // Avklaringsbehov må skje etterhverandre i tid, men testene kan kjøre så fort at alle får samme tidspunkt. Og da blir sorteringen feil
        // Mocker derfor LocalDateTime.now slik at alle kall kommer 1 minutt etter hverandre.
        val start = LocalDateTime.now()
        mockkStatic(LocalDateTime::class)
        val counter = AtomicInteger(0)
        every { LocalDateTime.now() } answers { start.plusMinutes(counter.getAndIncrement().toLong()) }

        every { trekkKlageServiceMock.klageErTrukket(any()) } returns false
        every { formkravRepositoryMock.hentHvisEksisterer(any()) } returns null
        every { avbrytRevurderingServiceMock.revurderingErAvbrutt(any()) } returns true
        every { avklaringsbehovServiceMock.oppdaterAvklaringsbehov(
            avklaringsbehovene = any(),
             definisjon = Definisjon.VURDER_FORMKRAV,
            vedtakBehøverVurdering = any(),
            erTilstrekkeligVurdert = any(),
            tilbakestillGrunnlag = any(),
            kontekst = any(),
        ) } returns Unit
        InMemoryAvklaringsbehovRepository.clearMemory()
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(LocalDateTime::class)
    }

    @Test
    fun `FormkravSteg-utfører skal gi avklaringsbehov VURDER_FORMKRAV om man ikke har vurdert dette formkravet enda`() {
        every { brevbestillingServiceMock.hentBestillinger(any(), any()) } returns listOf()
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1L),
            behandlingId = BehandlingId(1L),
            behandlingType = TypeBehandling.Klage,
            forrigeBehandlingId = null,
            vurderingType = VurderingType.IKKE_RELEVANT,
            rettighetsperiode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)),
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTATT_KLAGE)
        )

        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(1),
            definisjon = Definisjon.VURDER_FORMKRAV,
            funnetISteg = StegType.FORMKRAV,
            frist = LocalDate.now().plusDays(1),
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        val steg = FormkravSteg(
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            formkravRepository = formkravRepositoryMock,
            behandlingRepository = behandlingRepositoryMock,
            brevbestillingService = brevbestillingServiceMock,
            trekkKlageService = trekkKlageServiceMock,
            avklaringsbehovService = AvklaringsbehovService(
                avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
                avbrytRevurderingService = avbrytRevurderingServiceMock,
            ),
            unleashGateway = gatewayProvider.provide()
        )

        steg.utfør(kontekst)

        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(BehandlingId(1))
        assertThat(avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VURDER_FORMKRAV)?.definisjon).isEqualTo(Definisjon.VURDER_FORMKRAV)
    }

    @Test
    fun `FormkravSteg-utfører skal gi avklaringsbehov FULLFØRT om man har løst avklaringsbehovet og vurderingen er at man har opprettholdt avklaringsbehovene`() {
        every { brevbestillingServiceMock.hentBestillinger(any(), any()) } returns listOf(
            Brevbestilling(
                id = 1L,
                behandlingId = BehandlingId(1L),
                typeBrev = TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV,
                referanse = BrevbestillingReferanse(UUID.randomUUID()),
                status = Status.FULLFØRT,
                opprettet = LocalDateTime.now(),
            )
        )

        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1L),
            behandlingId = BehandlingId(2L),
            behandlingType = TypeBehandling.Klage,
            forrigeBehandlingId = null,
            vurderingType = VurderingType.IKKE_RELEVANT,
            rettighetsperiode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)),
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTATT_KLAGE)
        )

        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(2L),
            definisjon = Definisjon.VURDER_FORMKRAV,
            funnetISteg = StegType.FORMKRAV,
            frist = LocalDate.now().plusDays(1),
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        val avklaringsbehov = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(BehandlingId(2L))
        avklaringsbehov.løsAvklaringsbehov(
            definisjon = Definisjon.VURDER_FORMKRAV,
            begrunnelse = "Vurdert ok",
            endretAv = "Ident"
        )

        val steg = FormkravSteg(
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            formkravRepository = formkravRepositoryMock,
            behandlingRepository = behandlingRepositoryMock,
            brevbestillingService = brevbestillingServiceMock,
            trekkKlageService = trekkKlageServiceMock,
            avklaringsbehovService = AvklaringsbehovService(
                avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
                avbrytRevurderingService = avbrytRevurderingServiceMock,
            ),
            unleashGateway = gatewayProvider.provide()
        )


        every { formkravRepositoryMock.hentHvisEksisterer(any()) } returns FormkravGrunnlag(
            vurdering = FormkravVurdering(
                begrunnelse = "begrunnelse",
                erKonkret = true,
                erBrukerPart = true,
                erSignert = true,
                erFristOverholdt = true,
                likevelBehandles = null,
                vurdertAv = "indent",
                opprettet = Instant.now(),
            )
        )

        val resultat = steg.utfør(kontekst)

        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `FormkravSteg-utfører skal gi avklaringsbehov FULLFØRT om fristen ikke er overholdt, selv om man ikke har oppfylt formkravene`() {
        every { behandlingRepositoryMock.hent(any<BehandlingId>()) } returns tomBehandling(BehandlingId(1L))
        every { formkravRepositoryMock.lagreVarsel(any(), any()) } returns Unit

        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1L),
            behandlingId = BehandlingId(1L),
            behandlingType = TypeBehandling.Klage,
            forrigeBehandlingId = null,
            vurderingType = VurderingType.IKKE_RELEVANT,
            rettighetsperiode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)),
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTATT_KLAGE)
        )

        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(1L),
            definisjon = Definisjon.VURDER_FORMKRAV,
            funnetISteg = StegType.FORMKRAV,
            frist = LocalDate.now().plusDays(1),
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        val avklaringsbehov = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(BehandlingId(1L))
        avklaringsbehov.løsAvklaringsbehov(
            definisjon = Definisjon.VURDER_FORMKRAV,
            begrunnelse = "Vurdert ok",
            endretAv = "Ident"
        )

        val steg = FormkravSteg(
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            formkravRepository = formkravRepositoryMock,
            behandlingRepository = behandlingRepositoryMock,
            brevbestillingService = brevbestillingServiceMock,
            trekkKlageService = trekkKlageServiceMock,
            avklaringsbehovService = AvklaringsbehovService(
                avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
                avbrytRevurderingService = avbrytRevurderingServiceMock,
            ),
            unleashGateway = gatewayProvider.provide()
        )

        every { formkravRepositoryMock.hentHvisEksisterer(any()) } returns FormkravGrunnlag(
            vurdering = FormkravVurdering(
                begrunnelse = "begrunnelse",
                erKonkret = true,
                erBrukerPart = true,
                erSignert = false,
                erFristOverholdt = false,
                likevelBehandles = false,
                vurdertAv = "indent",
                opprettet = Instant.now(),
            )
        )

        every { brevbestillingServiceMock.hentBestillinger(any(), any()) } returns emptyList()
        every { brevbestillingServiceMock.bestillV2(any(),any(),any(),any(),) } returns UUID.randomUUID()

        val resultat = steg.utfør(kontekst)

        assertThat(resultat).isEqualTo(Fullført)
    }

    @Test
    fun `FormkravSteg-utfører skal gi avklaringsbehov SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV og bestille brev om man har lagret en vurdering med ikke-oppfylte formkrav`() {
        every { behandlingRepositoryMock.hent(any<BehandlingId>()) } returns tomBehandling(BehandlingId(1L))
        every { brevbestillingServiceMock.bestillV2(any(), any(), any(), any()) } returns UUID.randomUUID()
        every { formkravRepositoryMock.lagreVarsel(any(), any()) } returns Unit

        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1L),
            behandlingId = BehandlingId(1L),
            behandlingType = TypeBehandling.Klage,
            forrigeBehandlingId = null,
            vurderingType = VurderingType.IKKE_RELEVANT,
            rettighetsperiode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)),
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTATT_KLAGE)
        )

        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(1),
            definisjon = Definisjon.VURDER_FORMKRAV,
            funnetISteg = StegType.FORMKRAV,
            frist = LocalDate.now().plusDays(1),
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        val avklaringsbehov = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(BehandlingId(1L))
        avklaringsbehov.løsAvklaringsbehov(
            definisjon = Definisjon.VURDER_FORMKRAV,
            begrunnelse = "Vurdert ok",
            endretAv = "Ident"
        )

        val steg = FormkravSteg(
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            formkravRepository = formkravRepositoryMock,
            behandlingRepository = behandlingRepositoryMock,
            brevbestillingService = brevbestillingServiceMock,
            trekkKlageService = trekkKlageServiceMock,
            avklaringsbehovService = AvklaringsbehovService(
                avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
                avbrytRevurderingService = avbrytRevurderingServiceMock,
            ),
            unleashGateway = gatewayProvider.provide()
        )

        every { formkravRepositoryMock.hentHvisEksisterer(any()) } returns FormkravGrunnlag(
            vurdering = FormkravVurdering(
                begrunnelse = "begrunnelse",
                erKonkret = false,
                erBrukerPart = false,
                erSignert = false,
                erFristOverholdt = true,
                likevelBehandles = null,
                vurdertAv = "indent",
                opprettet = Instant.now(),
            ),
            varsel = FormkravVarsel(
                varselId = BrevbestillingReferanse(UUID.randomUUID()),
                sendtDato = LocalDate.now(),
                svarfrist = LocalDate.now().plusWeeks(3)
            )
        )

        every { brevbestillingServiceMock.hentBestillinger(any(), any()) } returns emptyList()

        steg.utfør(kontekst)
        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(BehandlingId(1))
        assertThat(avklaringsbehovene.hentBehovForDefinisjon(Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV)?.definisjon).isEqualTo(Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV)

        verify { brevbestillingServiceMock.bestillV2(BehandlingId(1L), any(), any(), any()) }
    }

    @Test
    fun `FormkravSteg-utfører skal gi avklaringsbehov SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV om det allerede er bestilt brev men brevet er ikke sendt`() {
        every { brevbestillingServiceMock.hentBestillinger(any(), any()) } returns listOf(
            Brevbestilling(
                id = 1L,
                behandlingId = BehandlingId(1L),
                typeBrev = TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV,
                referanse = BrevbestillingReferanse(UUID.randomUUID()),
                status = Status.FORHÅNDSVISNING_KLAR,
                opprettet = LocalDateTime.now(),
            )
        )

        every { formkravRepositoryMock.hentHvisEksisterer(any()) } returns FormkravGrunnlag(
            vurdering = FormkravVurdering(
                begrunnelse = "begrunnelse",
                erKonkret = false,
                erBrukerPart = true,
                erSignert = false,
                erFristOverholdt = true,
                likevelBehandles = null,
                vurdertAv = "indent",
                opprettet = Instant.now(),
            ),
            varsel = FormkravVarsel(
                varselId = BrevbestillingReferanse(UUID.randomUUID()),
                sendtDato = LocalDate.now(),
                svarfrist = LocalDate.now().plusWeeks(3)
            )
        )

        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1L),
            behandlingId = BehandlingId(1L),
            behandlingType = TypeBehandling.Klage,
            forrigeBehandlingId = null,
            vurderingType = VurderingType.IKKE_RELEVANT,
            rettighetsperiode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)),
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTATT_KLAGE)
        )

        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(1),
            definisjon = Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV,
            funnetISteg = StegType.FORMKRAV,
            frist = LocalDate.now().plusDays(1),
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(1),
            definisjon = Definisjon.VURDER_FORMKRAV,
            funnetISteg = StegType.FORMKRAV,
            frist = LocalDate.now().plusDays(1),
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        val avklaringsbehov = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(BehandlingId(1L))
        avklaringsbehov.løsAvklaringsbehov(
            definisjon = Definisjon.VURDER_FORMKRAV,
            begrunnelse = "Vurdert ok",
            endretAv = "Ident"
        )

        val steg = FormkravSteg(
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            formkravRepository = formkravRepositoryMock,
            behandlingRepository = behandlingRepositoryMock,
            brevbestillingService = brevbestillingServiceMock,
            trekkKlageService = trekkKlageServiceMock,
            avklaringsbehovService = AvklaringsbehovService(
                avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
                avbrytRevurderingService = avbrytRevurderingServiceMock,
            ),
            unleashGateway = gatewayProvider.provide()
        )

        val resultat = steg.utfør(kontekst)

        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(BehandlingId(1))
        assertThat(avklaringsbehovene.hentBehovForDefinisjon(Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV)?.definisjon).isEqualTo(Definisjon.SKRIV_FORHÅNDSVARSEL_KLAGE_FORMKRAV_BREV)
    }

    @Test
    fun `FormkravSteg-utfører skal gi ventebehov om brev er sendt og formkravet ikke er oppfyllt`() {
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1L),
            behandlingId = BehandlingId(1L),
            behandlingType = TypeBehandling.Klage,
            forrigeBehandlingId = null,
            vurderingType = VurderingType.IKKE_RELEVANT,
            rettighetsperiode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)),
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTATT_KLAGE)
        )

        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(1),
            definisjon = Definisjon.VURDER_FORMKRAV,
            funnetISteg = StegType.FORMKRAV,
            frist = LocalDate.now().plusDays(1),
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        val avklaringsbehov = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(BehandlingId(1L))
        avklaringsbehov.løsAvklaringsbehov(
            definisjon = Definisjon.VURDER_FORMKRAV,
            begrunnelse = "Vurdert ok",
            endretAv = "Ident"
        )

        val steg = FormkravSteg(
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            formkravRepository = formkravRepositoryMock,
            behandlingRepository = behandlingRepositoryMock,
            brevbestillingService = brevbestillingServiceMock,
            trekkKlageService = trekkKlageServiceMock,
            avklaringsbehovService = AvklaringsbehovService(
                avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
                avbrytRevurderingService = avbrytRevurderingServiceMock,
            ),
            unleashGateway = gatewayProvider.provide()
        )

        every { formkravRepositoryMock.hentHvisEksisterer(any()) } returns FormkravGrunnlag(
            vurdering = FormkravVurdering(
                begrunnelse = "begrunnelse",
                erKonkret = false,
                erBrukerPart = false,
                erSignert = false,
                erFristOverholdt = true,
                likevelBehandles = false,
                vurdertAv = "indent",
                opprettet = Instant.now(),
            ),
            varsel = FormkravVarsel(
                varselId = BrevbestillingReferanse(UUID.randomUUID()),
                sendtDato = LocalDate.now(),
                svarfrist = LocalDate.now().plusWeeks(3)
            )
        )

        every { brevbestillingServiceMock.hentBestillinger(any(), any()) } returns listOf(
            forhåndsvarselBrevbestilling(
                1L,
                1L
            )
        )


        val resultat = steg.utfør(kontekst)

        assertThat(resultat).isInstanceOf(FantVentebehov::class.java)
    }

    @Test
    fun `FormkravSteg-utfører skal gi FULLFØRT om varsel er sendt og formkrav ikke er oppfyllt - men men er over svarfristen på 3 uker`() {
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1L),
            behandlingId = BehandlingId(1L),
            behandlingType = TypeBehandling.Klage,
            forrigeBehandlingId = null,
            vurderingType = VurderingType.IKKE_RELEVANT,
            rettighetsperiode = Periode(LocalDate.now().minusDays(1), LocalDate.now().plusYears(1)),
            vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.MOTATT_KLAGE)
        )

        InMemoryAvklaringsbehovRepository.opprett(
            BehandlingId(1),
            definisjon = Definisjon.VURDER_FORMKRAV,
            funnetISteg = StegType.FORMKRAV,
            frist = LocalDate.now().plusDays(1),
            begrunnelse = "Begrunnelse",
            endretAv = "Ident",
        )

        val avklaringsbehov = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(BehandlingId(1L))
        avklaringsbehov.løsAvklaringsbehov(
            definisjon = Definisjon.VURDER_FORMKRAV,
            begrunnelse = "Vurdert ok",
            endretAv = "Ident"
        )

        val steg = FormkravSteg(
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            formkravRepository = formkravRepositoryMock,
            behandlingRepository = behandlingRepositoryMock,
            brevbestillingService = brevbestillingServiceMock,
            trekkKlageService = trekkKlageServiceMock,
            avklaringsbehovService = AvklaringsbehovService(
                avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
                avbrytRevurderingService = avbrytRevurderingServiceMock,
            ),
            unleashGateway = gatewayProvider.provide()
        )

        every { formkravRepositoryMock.hentHvisEksisterer(any()) } returns FormkravGrunnlag(
            vurdering = FormkravVurdering(
                begrunnelse = "begrunnelse",
                erKonkret = false,
                erBrukerPart = false,
                erSignert = false,
                erFristOverholdt = true,
                likevelBehandles = true,
                vurdertAv = "indent",
                opprettet = Instant.now(),
            ),
            varsel = FormkravVarsel(
                varselId = BrevbestillingReferanse(UUID.randomUUID()),
                sendtDato = LocalDate.now().minusWeeks(4),
                svarfrist = LocalDate.now().minusWeeks(3)
            )
        )

        every { brevbestillingServiceMock.hentBestillinger(any(), any()) } returns listOf(
            forhåndsvarselBrevbestilling(
                1L,
                1L
            )
        )

        val resultat = steg.utfør(kontekst)
        assertThat(resultat).isEqualTo(Fullført)
    }

    private fun tomBehandling(behandlingId: BehandlingId) = Behandling(
        behandlingId,
        sakId = SakId(1),
        typeBehandling = TypeBehandling.Førstegangsbehandling,
        årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
        forrigeBehandlingId = null,
        versjon = 1
    )

    private fun forhåndsvarselBrevbestilling(id: Long, behandlingId: Long) = Brevbestilling(
        id = id,
        behandlingId = BehandlingId(behandlingId),
        typeBrev = TypeBrev.FORHÅNDSVARSEL_KLAGE_FORMKRAV,
        referanse = BrevbestillingReferanse(UUID.randomUUID()),
        status = Status.FULLFØRT,
        opprettet = LocalDateTime.now()
    )
}