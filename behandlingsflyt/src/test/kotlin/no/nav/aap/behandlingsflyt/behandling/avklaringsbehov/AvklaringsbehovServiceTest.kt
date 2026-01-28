package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import io.mockk.mockk
import io.mockk.verify
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvbrytRevurderingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class AvklaringsbehovServiceTest {

    private val avklaringsbehovRepository = InMemoryAvklaringsbehovRepository
    private val vilkårsresultatRepository = InMemoryVilkårsresultatRepository
    private val avbrytRevurderingService = AvbrytRevurderingService(InMemoryAvbrytRevurderingRepository)
    private val trukketSøknadRepository = InMemoryTrukketSøknadRepository
    private lateinit var avklaringsbehovService: AvklaringsbehovService

    @BeforeEach
    fun setup() {
        avklaringsbehovService = AvklaringsbehovService(
            avbrytRevurderingService = avbrytRevurderingService,
            avklaringsbehovRepository = InMemoryAvklaringsbehovRepository,
            behandlingRepository = InMemoryBehandlingRepository,
            vilkårsresultatRepository = vilkårsresultatRepository,
            trukketSøknadService = TrukketSøknadService(trukketSøknadRepository)
        )
    }

    @Test
    fun `oppdaterAvklaringsbehov skal opprette nytt avklaringsbehov når vedtak behøver vurdering og ingen eksisterer`() {
        // Arrange
        val behandlingId = BehandlingId(1001)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
        val vedtakBehøverVurdering = { true }
        val erTilstrekkeligVurdert = { false }
        val tilbakestillGrunnlag = mockk<() -> Unit>(relaxed = true)
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1),
            behandlingId = behandlingId,
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = Periode(LocalDate.now(), Tid.MAKS),
            vurderingsbehovRelevanteForSteg = emptySet()
        )

        // Act
        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = definisjon,
            vedtakBehøverVurdering = vedtakBehøverVurdering,
            erTilstrekkeligVurdert = erTilstrekkeligVurdert,
            tilbakestillGrunnlag = tilbakestillGrunnlag,
            kontekst = kontekst
        )

        // Assert
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov).isNotNull
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.OPPRETTET)
        assertThat(avklaringsbehov?.definisjon).isEqualTo(definisjon)
        verify(exactly = 0) { tilbakestillGrunnlag() }
    }

    @Test
    fun `oppdaterAvklaringsbehov skal avslutte avklaringsbehov når vedtak behøver vurdering og er tilstrekkelig vurdert`() {
        // Arrange
        val behandlingId = BehandlingId(1002)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
        avklaringsbehovene.leggTil(definisjon, definisjon.løsesISteg, null, null)
        avklaringsbehovene.løsAvklaringsbehov(definisjon, begrunnelse = "Test", endretAv = "Tester")

        val vedtakBehøverVurdering = { true }
        val erTilstrekkeligVurdert = { true }
        val tilbakestillGrunnlag = mockk<() -> Unit>(relaxed = true)
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1),
            behandlingId = behandlingId,
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = Periode(LocalDate.now(), Tid.MAKS),
            vurderingsbehovRelevanteForSteg = emptySet()
        )

        // Act
        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = definisjon,
            vedtakBehøverVurdering = vedtakBehøverVurdering,
            erTilstrekkeligVurdert = erTilstrekkeligVurdert,
            tilbakestillGrunnlag = tilbakestillGrunnlag,
            kontekst = kontekst
        )

        // Assert
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.AVSLUTTET)
        verify(exactly = 0) { tilbakestillGrunnlag() }
    }

    @Test
    fun `oppdaterAvklaringsbehov skal avbryte avklaringsbehov når vedtak ikke behøver vurdering`() {
        // Arrange
        val behandlingId = BehandlingId(1003)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
        avklaringsbehovene.leggTil(definisjon, definisjon.løsesISteg, null, null)

        val vedtakBehøverVurdering = { false }
        val erTilstrekkeligVurdert = { false }
        val tilbakestillGrunnlag = mockk<() -> Unit>(relaxed = true)
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1),
            behandlingId = behandlingId,
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = Periode(LocalDate.now(), Tid.MAKS),
            vurderingsbehovRelevanteForSteg = emptySet()
        )

        // Act
        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = definisjon,
            vedtakBehøverVurdering = vedtakBehøverVurdering,
            erTilstrekkeligVurdert = erTilstrekkeligVurdert,
            tilbakestillGrunnlag = tilbakestillGrunnlag,
            kontekst = kontekst
        )

        // Assert
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.AVBRUTT)
        verify(exactly = 1) { tilbakestillGrunnlag() }
    }

    @Test
    fun `oppdaterAvklaringsbehov skal ikke gjøre noe når vedtak ikke behøver vurdering og ingen avklaringsbehov finnes`() {
        // Arrange
        val behandlingId = BehandlingId(1004)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
        val vedtakBehøverVurdering = { false }
        val erTilstrekkeligVurdert = { false }
        val tilbakestillGrunnlag = mockk<() -> Unit>(relaxed = true)
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1),
            behandlingId = behandlingId,
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = Periode(LocalDate.now(), Tid.MAKS),
            vurderingsbehovRelevanteForSteg = emptySet()
        )

        // Act
        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = definisjon,
            vedtakBehøverVurdering = vedtakBehøverVurdering,
            erTilstrekkeligVurdert = erTilstrekkeligVurdert,
            tilbakestillGrunnlag = tilbakestillGrunnlag,
            kontekst = kontekst
        )

        // Assert
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov).isNull()
        verify(exactly = 0) { tilbakestillGrunnlag() }
    }

    @Test
    fun `oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår skal opprette avklaringsbehov når vurdering er relevant`() {
        val behandlingId = BehandlingId(2001)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
        val tvingerAvklaringsbehov = setOf(Vurderingsbehov.MOTTATT_SØKNAD)
        val startDato = LocalDate.of(2024, 1, 1)
        val periode1 = Periode(startDato, startDato.plusMonths(2).minusDays(1))
        val periode2 = Periode(startDato.plusMonths(2), startDato.plusMonths(4).minusDays(1))
        val periode3 = Periode(startDato.plusMonths(4), startDato.plusMonths(6))
        val helePeriode = Periode(startDato, startDato.plusMonths(6))

        val nårVurderingErRelevant: (FlytKontekstMedPerioder) -> Tidslinje<Boolean> = {
            Tidslinje(
                listOf(
                    Segment(periode1, true),
                    Segment(periode2, true),
                    Segment(periode3, false)
                )
            )
        }
        val nårVurderingErGyldig: () -> Tidslinje<Boolean> = {
            Tidslinje(
                listOf(
                    Segment(periode1, false),
                    Segment(periode2, false),
                    Segment(periode3, true)
                )
            )
        }
        val tilbakestillGrunnlag = mockk<() -> Unit>(relaxed = true)
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1),
            behandlingId = behandlingId,
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = helePeriode,
            vurderingsbehovRelevanteForSteg = tvingerAvklaringsbehov
        )

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = definisjon,
            tvingerAvklaringsbehov = tvingerAvklaringsbehov,
            nårVurderingErRelevant = nårVurderingErRelevant,
            nårVurderingErGyldig = nårVurderingErGyldig,
            kontekst = kontekst,
            tilbakestillGrunnlag = tilbakestillGrunnlag
        )

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov).isNotNull
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.OPPRETTET)
    }

    @Test
    fun `oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår skal ikke opprette avklaringsbehov når vurdering ikke er relevant`() {
        val behandlingId = BehandlingId(2002)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
        val tvingerAvklaringsbehov = setOf(Vurderingsbehov.MOTTATT_SØKNAD)
        val startDato = LocalDate.of(2024, 1, 1)
        val periode1 = Periode(startDato, startDato.plusMonths(1).minusDays(1))
        val periode2 = Periode(startDato.plusMonths(1), startDato.plusMonths(3))
        val helePeriode = Periode(startDato, startDato.plusMonths(3))

        val nårVurderingErRelevant: (FlytKontekstMedPerioder) -> Tidslinje<Boolean> = {
            Tidslinje(
                listOf(
                    Segment(periode1, false),
                    Segment(periode2, false)
                )
            )
        }
        val nårVurderingErGyldig: () -> Tidslinje<Boolean> = {
            Tidslinje(
                listOf(
                    Segment(periode1, true),
                    Segment(periode2, true)
                )
            )
        }
        val tilbakestillGrunnlag = mockk<() -> Unit>(relaxed = true)
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1),
            behandlingId = behandlingId,
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = helePeriode,
            vurderingsbehovRelevanteForSteg = tvingerAvklaringsbehov
        )

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = definisjon,
            tvingerAvklaringsbehov = tvingerAvklaringsbehov,
            nårVurderingErRelevant = nårVurderingErRelevant,
            nårVurderingErGyldig = nårVurderingErGyldig,
            kontekst = kontekst,
            tilbakestillGrunnlag = tilbakestillGrunnlag
        )

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov).isNull()
    }

    @Test
    fun `oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår skal avbryte avklaringsbehov når vurdering ikke lenger er relevant`() {
        val behandlingId = BehandlingId(2003)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
        avklaringsbehovene.leggTil(definisjon, definisjon.løsesISteg, null, null)

        val tvingerAvklaringsbehov = setOf(Vurderingsbehov.MOTTATT_SØKNAD)
        val startDato = LocalDate.of(2024, 2, 1)
        val periode1 = Periode(startDato, startDato.plusMonths(2).minusDays(1))
        val periode2 = Periode(startDato.plusMonths(2), startDato.plusMonths(5))
        val helePeriode = Periode(startDato, startDato.plusMonths(5))

        val nårVurderingErRelevant: (FlytKontekstMedPerioder) -> Tidslinje<Boolean> = {
            Tidslinje(
                listOf(
                    Segment(periode1, false),
                    Segment(periode2, false)
                )
            )
        }
        val nårVurderingErGyldig: () -> Tidslinje<Boolean> = {
            Tidslinje(
                listOf(
                    Segment(periode1, true),
                    Segment(periode2, true)
                )
            )
        }
        val tilbakestillGrunnlag = mockk<() -> Unit>(relaxed = true)
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1),
            behandlingId = behandlingId,
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = helePeriode,
            vurderingsbehovRelevanteForSteg = tvingerAvklaringsbehov
        )

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = definisjon,
            tvingerAvklaringsbehov = tvingerAvklaringsbehov,
            nårVurderingErRelevant = nårVurderingErRelevant,
            nårVurderingErGyldig = nårVurderingErGyldig,
            kontekst = kontekst,
            tilbakestillGrunnlag = tilbakestillGrunnlag
        )

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.AVBRUTT)
        verify(exactly = 1) { tilbakestillGrunnlag() }
    }

    @Test
    fun `oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår skal opprette avklaringsbehov når noen perioder krever vurdering`() {
        val behandlingId = BehandlingId(2004)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
        val tvingerAvklaringsbehov = setOf(Vurderingsbehov.MOTTATT_SØKNAD)
        val startDato = LocalDate.of(2024, 3, 1)
        val periode1 = Periode(startDato, startDato.plusMonths(1).minusDays(1))
        val periode2 = Periode(startDato.plusMonths(1), startDato.plusMonths(2).minusDays(1))
        val periode3 = Periode(startDato.plusMonths(2), startDato.plusMonths(4))
        val helePeriode = Periode(startDato, startDato.plusMonths(4))

        val nårVurderingErRelevant: (FlytKontekstMedPerioder) -> Tidslinje<Boolean> = {
            Tidslinje(
                listOf(
                    Segment(periode1, false),
                    Segment(periode2, true),
                    Segment(periode3, false)
                )
            )
        }
        val nårVurderingErGyldig: () -> Tidslinje<Boolean> = {
            Tidslinje(
                listOf(
                    Segment(periode1, true),
                    Segment(periode2, false),
                    Segment(periode3, true)
                )
            )
        }
        val tilbakestillGrunnlag = mockk<() -> Unit>(relaxed = true)
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1),
            behandlingId = behandlingId,
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = helePeriode,
            vurderingsbehovRelevanteForSteg = tvingerAvklaringsbehov
        )

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = definisjon,
            tvingerAvklaringsbehov = tvingerAvklaringsbehov,
            nårVurderingErRelevant = nårVurderingErRelevant,
            nårVurderingErGyldig = nårVurderingErGyldig,
            kontekst = kontekst,
            tilbakestillGrunnlag = tilbakestillGrunnlag
        )

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov).isNotNull
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.OPPRETTET)
    }

    @Test
    fun `oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert skal opprette avklaringsbehov for utilstrekkelig vurderte perioder`() {
        val behandlingId = BehandlingId(2006)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
        val tvingerAvklaringsbehov = setOf(Vurderingsbehov.MOTTATT_SØKNAD)
        val startDato = LocalDate.of(2024, 5, 1)
        val periode1 = Periode(startDato, startDato.plusMonths(1).minusDays(1))
        val periode2 = Periode(startDato.plusMonths(1), startDato.plusMonths(2).minusDays(1))
        val periode3 = Periode(startDato.plusMonths(2), startDato.plusMonths(3))
        val helePeriode = Periode(startDato, startDato.plusMonths(3))

        val nårVurderingErRelevant: (FlytKontekstMedPerioder) -> Tidslinje<Boolean> = {
            Tidslinje(
                listOf(
                    Segment(periode1, true),
                    Segment(periode2, true),
                    Segment(periode3, true)
                )
            )
        }
        val perioderSomIkkeErTilstrekkeligVurdert = setOf(periode2, periode3)
        val tilbakestillGrunnlag = mockk<() -> Unit>(relaxed = true)
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1),
            behandlingId = behandlingId,
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = helePeriode,
            vurderingsbehovRelevanteForSteg = tvingerAvklaringsbehov
        )

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
            definisjon = definisjon,
            tvingerAvklaringsbehov = tvingerAvklaringsbehov,
            nårVurderingErRelevant = nårVurderingErRelevant,
            kontekst = kontekst,
            perioderSomIkkeErTilstrekkeligVurdert = { perioderSomIkkeErTilstrekkeligVurdert },
            tilbakestillGrunnlag = tilbakestillGrunnlag
        )

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov).isNotNull
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.OPPRETTET)
    }

    @Test
    fun `oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert skal avslutte avklaringsbehov når alle perioder er tilstrekkelig vurdert`() {
        val behandlingId = BehandlingId(2007)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
        avklaringsbehovene.leggTil(definisjon, definisjon.løsesISteg, null, null)
        avklaringsbehovene.løsAvklaringsbehov(definisjon, begrunnelse = "Test", endretAv = "Tester")

        val tvingerAvklaringsbehov = setOf(Vurderingsbehov.MOTTATT_SØKNAD)
        val startDato = LocalDate.of(2024, 6, 1)
        val periode1 = Periode(startDato, startDato.plusMonths(1).minusDays(1))
        val periode2 = Periode(startDato.plusMonths(1), startDato.plusMonths(2))
        val helePeriode = Periode(startDato, startDato.plusMonths(2))

        val nårVurderingErRelevant: (FlytKontekstMedPerioder) -> Tidslinje<Boolean> = {
            Tidslinje(
                listOf(
                    Segment(periode1, true),
                    Segment(periode2, true)
                )
            )
        }
        val perioderSomIkkeErTilstrekkeligVurdert = emptySet<Periode>()
        val tilbakestillGrunnlag = mockk<() -> Unit>(relaxed = true)
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1),
            behandlingId = behandlingId,
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = helePeriode,
            vurderingsbehovRelevanteForSteg = tvingerAvklaringsbehov
        )

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
            definisjon = definisjon,
            tvingerAvklaringsbehov = tvingerAvklaringsbehov,
            nårVurderingErRelevant = nårVurderingErRelevant,
            kontekst = kontekst,
            perioderSomIkkeErTilstrekkeligVurdert = { perioderSomIkkeErTilstrekkeligVurdert },
            tilbakestillGrunnlag = tilbakestillGrunnlag
        )

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `oppdaterAvklaringsbehov skal ikke default tilbakestille frivillige avklaringsbehov`() {
        // Arrange
        val behandlingId = BehandlingId(1003)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SAMORDNING_UFØRE
        avklaringsbehovene.leggTil(definisjon, definisjon.løsesISteg, null, null)
        avklaringsbehovene.løsAvklaringsbehov(definisjon, begrunnelse = "Test", endretAv = "Tester")

        val vedtakBehøverVurdering = { false }
        val erTilstrekkeligVurdert = { false }
        val tilbakestillGrunnlag = mockk<() -> Unit>(relaxed = true)
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1),
            behandlingId = behandlingId,
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = Periode(LocalDate.now(), Tid.MAKS),
            vurderingsbehovRelevanteForSteg = emptySet()
        )

        // Act
        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = definisjon,
            vedtakBehøverVurdering = vedtakBehøverVurdering,
            erTilstrekkeligVurdert = erTilstrekkeligVurdert,
            tilbakestillGrunnlag = tilbakestillGrunnlag,
            kontekst = kontekst
        )

        // Assert
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.AVSLUTTET)
        verify(exactly = 0) { tilbakestillGrunnlag() }
    }

    @Test
    fun `oppdaterAvklaringsbehov skal tilbakestille frivillige avklaringsbehov ved søknadstrekking`() {
        // Arrange
        val behandlingId = BehandlingId(1003)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SAMORDNING_UFØRE
        avklaringsbehovene.leggTil(definisjon, definisjon.løsesISteg, null, null)
        avklaringsbehovene.løsAvklaringsbehov(definisjon, begrunnelse = "Test", endretAv = "Tester")

        val vedtakBehøverVurdering = { false }
        val erTilstrekkeligVurdert = { false }
        val tilbakestillGrunnlag = mockk<() -> Unit>(relaxed = true)
        val kontekst = FlytKontekstMedPerioder(
            sakId = SakId(1),
            behandlingId = behandlingId,
            forrigeBehandlingId = null,
            behandlingType = TypeBehandling.Førstegangsbehandling,
            vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
            rettighetsperiode = Periode(LocalDate.now(), Tid.MAKS),
            vurderingsbehovRelevanteForSteg = emptySet()
        )
        trukketSøknadRepository.lagreTrukketSøknadVurdering(
            behandlingId,
            TrukketSøknadVurdering(
                journalpostId = JournalpostId("12344321"),
                begrunnelse = "en grunn",
                vurdertAv = Bruker("Z00000"),
                skalTrekkes = true,
                vurdert = Instant.parse("2020-01-01T12:12:12Z"),
            )
        )

        // Act
        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = definisjon,
            vedtakBehøverVurdering = vedtakBehøverVurdering,
            erTilstrekkeligVurdert = erTilstrekkeligVurdert,
            tilbakestillGrunnlag = tilbakestillGrunnlag,
            kontekst = kontekst
        )

        // Assert
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.AVBRUTT)
        verify(exactly = 1) { tilbakestillGrunnlag() }
    }
}
