package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.PeriodisertVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.genererVilkårsresultat
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgRevurdering
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon.AVKLAR_BISTANDSBEHOV
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.test.AlleAvskruddUnleash
import no.nav.aap.behandlingsflyt.test.LokalUnleash
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryVilkårsresultatRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class AvklaringsbehovServiceTest {

    private val avklaringsbehovRepository = InMemoryAvklaringsbehovRepository
    private val trukketSøknadRepository = InMemoryTrukketSøknadRepository
    private lateinit var avklaringsbehovService: AvklaringsbehovService
    private lateinit var avklaringsbehovServiceMedKrav: AvklaringsbehovService

    @BeforeEach
    fun setup() {
        avklaringsbehovService = AvklaringsbehovService(
            inMemoryRepositoryProvider,
            createGatewayProvider { register<AlleAvskruddUnleash>() }
        )
        avklaringsbehovServiceMedKrav = AvklaringsbehovService(
            inMemoryRepositoryProvider,
            createGatewayProvider { register<LokalUnleash>() }
        )
    }

    @Test
    fun `hvis det finnes vurdering i behandling, så avbrytes ikke frivillig avklaringsbehov`() {
        val (sak, _, revurdering) = opprettInMemorySakOgRevurdering()
        val avklaringsbehovene = Avklaringsbehovene(InMemoryAvklaringsbehovRepository, revurdering.id)

        avklaringsbehovene.leggTilFrivilligHvisMangler(Definisjon.FRITAK_MELDEPLIKT, Bruker("Z000"))
        avklaringsbehovene.løsAvklaringsbehov(Definisjon.FRITAK_MELDEPLIKT, "løsning fra intet", "Z000")

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = Definisjon.FRITAK_MELDEPLIKT,
            tvingerAvklaringsbehov = setOf(),
            nårVurderingErRelevant = { tidslinjeOf(sak.rettighetsperiode to true) },
            kontekst = flytKontekstMedPerioder { this.behandling = revurdering },
            nårVurderingErGyldig = { tidslinjeOf(sak.rettighetsperiode to true) },
            tilbakestillGrunnlag = { error("skal ikke tilbakestilles") },
            gjeldendeVurderinger = {
                tidslinjeOf(
                    sak.rettighetsperiode to (object : PeriodisertVurdering {
                        override val fom = sak.rettighetsperiode.fom
                        override val tom = null
                        override val vurdertIBehandling = revurdering.id
                        override val opprettet = Instant.now()
                    })
                )
            }
        )

        assertThat(
            Avklaringsbehovene(InMemoryAvklaringsbehovRepository, revurdering.id)
                .hentBehovForDefinisjon(Definisjon.FRITAK_MELDEPLIKT)!!.status()
        )
            .isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `oppretter ikke frivillig avklaringsbehov selv om det er en endring i relevans fra forrige behandling`() {
        val (sak, _, revurdering) = opprettInMemorySakOgRevurdering()

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = Definisjon.FRITAK_MELDEPLIKT,
            tvingerAvklaringsbehov = setOf(),
            nårVurderingErRelevant = {
                if (it.behandlingId == revurdering.id)
                    tidslinjeOf(sak.rettighetsperiode to true)
                else tidslinjeOf()
            },
            kontekst = flytKontekstMedPerioder { this.behandling = revurdering },
            nårVurderingErGyldig = { tidslinjeOf() },
            tilbakestillGrunnlag = { error("skal ikke tilbakestilles") },
            gjeldendeVurderinger = { tidslinjeOf() }
        )

        assertThat(
            Avklaringsbehovene(InMemoryAvklaringsbehovRepository, revurdering.id)
                .hentBehovForDefinisjon(Definisjon.FRITAK_MELDEPLIKT)
        )
            .isNull()
    }

    @Test
    fun `løfter frivillig avklaringsbehov hvis det blir tvunget av vurderingsbehov, selv ved ingen endring i relevans`() {
        val (sak, _, revurdering) = opprettInMemorySakOgRevurdering(
            vurderingsbehov = listOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)
        )

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = Definisjon.FRITAK_MELDEPLIKT,
            tvingerAvklaringsbehov = setOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND /* dette er feil vurderingsbehov, men vi kjører ikke flyten, så har ikke noe å si. */),
            nårVurderingErRelevant = { tidslinjeOf(sak.rettighetsperiode to true) },
            kontekst = flytKontekstMedPerioder { this.behandling = revurdering },
            nårVurderingErGyldig = { tidslinjeOf() },
            tilbakestillGrunnlag = { error("skal ikke tilbakestilles") },
            gjeldendeVurderinger = { tidslinjeOf() }
        )

        assertThat(
            Avklaringsbehovene(InMemoryAvklaringsbehovRepository, revurdering.id)
                .hentBehovForDefinisjon(Definisjon.FRITAK_MELDEPLIKT)
                ?.status()
        )
            .isEqualTo(Status.OPPRETTET)
    }


    @Test
    fun `oppdaterAvklaringsbehov skal opprette nytt avklaringsbehov når vedtak behøver vurdering og ingen eksisterer`() {
        // Arrange
        val behandlingId = BehandlingId(1001)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
        val vedtakBehøverVurdering = { true }
        val erTilstrekkeligVurdert = { false }
        val kontekst = flytKontekstMedPerioder {
            this.behandlingId = behandlingId
            this.rettighetsperiode = Periode(LocalDate.now(), Tid.MAKS)
        }

        // Act
        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = definisjon,
            vedtakBehøverVurdering = vedtakBehøverVurdering,
            erTilstrekkeligVurdert = erTilstrekkeligVurdert,
            tilbakestillGrunnlag = { error("skal ikke tilbakestilles") },
            kontekst = kontekst
        )

        // Assert
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov).isNotNull
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.OPPRETTET)
        assertThat(avklaringsbehov?.definisjon).isEqualTo(definisjon)
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
        val kontekst = flytKontekstMedPerioder {
            this.behandlingId = behandlingId
            this.rettighetsperiode = Periode(LocalDate.now(), Tid.MAKS)
        }

        // Act
        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = definisjon,
            vedtakBehøverVurdering = vedtakBehøverVurdering,
            erTilstrekkeligVurdert = erTilstrekkeligVurdert,
            tilbakestillGrunnlag = { error("skal ikke tilbakestilles") },
            kontekst = kontekst,
        )

        // Assert
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.AVSLUTTET)
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
        var erTilbakestilt = false
        val kontekst = flytKontekstMedPerioder {
            this.behandlingId = behandlingId
            this.rettighetsperiode = Periode(LocalDate.now(), Tid.MAKS)
        }

        // Act
        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = definisjon,
            vedtakBehøverVurdering = vedtakBehøverVurdering,
            erTilstrekkeligVurdert = erTilstrekkeligVurdert,
            tilbakestillGrunnlag = { erTilbakestilt = true },
            kontekst = kontekst
        )

        // Assert
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.AVBRUTT)
        assertThat(erTilbakestilt).isTrue
    }

    @Test
    fun `oppdaterAvklaringsbehov skal ikke gjøre noe når vedtak ikke behøver vurdering og ingen avklaringsbehov finnes`() {
        // Arrange
        val behandlingId = BehandlingId(1004)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
        val vedtakBehøverVurdering = { false }
        val erTilstrekkeligVurdert = { false }
        val kontekst = flytKontekstMedPerioder {
            this.behandlingId = behandlingId
            this.rettighetsperiode = Periode(LocalDate.now(), Tid.MAKS)
        }

        // Act
        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = definisjon,
            vedtakBehøverVurdering = vedtakBehøverVurdering,
            erTilstrekkeligVurdert = erTilstrekkeligVurdert,
            tilbakestillGrunnlag = { error("skal ikke tilbakestilles") },
            kontekst = kontekst
        )

        // Assert
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov).isNull()
    }

    @Test
    fun `oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår skal opprette avklaringsbehov når vurdering er relevant`() {
        val sak = opprettInMemorySak()
        val behandlingId = BehandlingId(2001)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
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
        val kontekst = flytKontekstMedPerioder {
            this.sakId = sak.id
            this.behandlingId = behandlingId
            this.rettighetsperiode = helePeriode
        }

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = definisjon,
            tvingerAvklaringsbehov = emptySet(),
            nårVurderingErRelevant = nårVurderingErRelevant,
            nårVurderingErGyldig = nårVurderingErGyldig,
            kontekst = kontekst,
            tilbakestillGrunnlag = { error("skal ikke tilbakestilles") },
        )

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov).isNotNull
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.OPPRETTET)
    }

    @Test
    fun `oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår skal ikke opprette avklaringsbehov når vurdering ikke er relevant`() {
        val sak = opprettInMemorySak()
        val behandlingId = BehandlingId(2002)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
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

        val kontekst = flytKontekstMedPerioder {
            this.sakId = sak.id
            this.behandlingId = behandlingId
            this.rettighetsperiode = helePeriode
        }

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = definisjon,
            tvingerAvklaringsbehov = emptySet(),
            nårVurderingErRelevant = nårVurderingErRelevant,
            nårVurderingErGyldig = nårVurderingErGyldig,
            kontekst = kontekst,
            tilbakestillGrunnlag = { error("skal ikke tilbakestilles") },
        )

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov).isNull()
    }

    @Test
    fun `oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår skal avbryte avklaringsbehov når vurdering ikke lenger er relevant`() {
        val sak = opprettInMemorySak()

        val behandlingId = BehandlingId(2003)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
        avklaringsbehovene.leggTil(definisjon, definisjon.løsesISteg, null, null)

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
        var erTilbakestilt = false

        val kontekst = flytKontekstMedPerioder {
            this.sakId = sak.id
            this.behandlingId = behandlingId
            this.rettighetsperiode = helePeriode
        }

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = definisjon,
            tvingerAvklaringsbehov = emptySet(),
            nårVurderingErRelevant = nårVurderingErRelevant,
            nårVurderingErGyldig = nårVurderingErGyldig,
            kontekst = kontekst,
            tilbakestillGrunnlag = { erTilbakestilt = true },
        )

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.AVBRUTT)
        assertThat(erTilbakestilt).isTrue
    }

    @Test
    fun `oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår skal opprette avklaringsbehov når noen perioder krever vurdering`() {
        val sak = opprettInMemorySak()
        val behandlingId = BehandlingId(2004)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
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
        val kontekst = flytKontekstMedPerioder {
            this.sakId = sak.id
            this.behandlingId = behandlingId
            this.rettighetsperiode = helePeriode
        }

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = definisjon,
            tvingerAvklaringsbehov = emptySet(),
            nårVurderingErRelevant = nårVurderingErRelevant,
            nårVurderingErGyldig = nårVurderingErGyldig,
            kontekst = kontekst,
            tilbakestillGrunnlag = { error("skal ikke tilbakestilles") },
        )

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov).isNotNull
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.OPPRETTET)
    }

    @Test
    fun `oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert skal opprette avklaringsbehov for utilstrekkelig vurderte perioder`() {
        val sak = opprettInMemorySak()
        val behandlingId = BehandlingId(2006)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
        val startDato = LocalDate.of(2024, 5, 1)
        val periode1 = Periode(startDato, startDato.plusMonths(1).minusDays(1))
        val periode2 = Periode(startDato.plusMonths(1), startDato.plusMonths(2).minusDays(1))
        val periode3 = Periode(startDato.plusMonths(2), startDato.plusMonths(3))
        val helePeriode = Periode(startDato, startDato.plusMonths(3))

        val nårVurderingErRelevant: (FlytKontekstMedPerioder) -> Tidslinje<Boolean> = {
            Tidslinje(
                listOf(
                    Segment(periode1, true), Segment(periode2, true), Segment(periode3, true)
                )
            )
        }
        val perioderSomIkkeErTilstrekkeligVurdert = setOf(periode2, periode3)

        val kontekst = flytKontekstMedPerioder {
            this.sakId = sak.id
            this.behandlingId = behandlingId
            this.rettighetsperiode = helePeriode
        }

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
            definisjon = definisjon,
            tvingerAvklaringsbehov = emptySet(),
            nårVurderingErRelevant = nårVurderingErRelevant,
            kontekst = kontekst,
            perioderSomIkkeErTilstrekkeligVurdert = { perioderSomIkkeErTilstrekkeligVurdert },
            tilbakestillGrunnlag = { error("skal ikke tilbakestilles") },
        )

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov).isNotNull
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.OPPRETTET)
    }

    @Test
    fun `oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert skal avslutte avklaringsbehov når alle perioder er tilstrekkelig vurdert`() {
        val sak = opprettInMemorySak()
        val behandlingId = BehandlingId(2007)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
        avklaringsbehovene.leggTil(definisjon, definisjon.løsesISteg, null, null)
        avklaringsbehovene.løsAvklaringsbehov(definisjon, begrunnelse = "Test", endretAv = "Tester")

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

        val kontekst = flytKontekstMedPerioder {
            this.sakId = sak.id
            this.behandlingId = behandlingId
            this.rettighetsperiode = helePeriode
        }

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
            definisjon = definisjon,
            tvingerAvklaringsbehov = emptySet(),
            nårVurderingErRelevant = nårVurderingErRelevant,
            kontekst = kontekst,
            perioderSomIkkeErTilstrekkeligVurdert = { perioderSomIkkeErTilstrekkeligVurdert },
            tilbakestillGrunnlag = { error("skal ikke tilbakestilles") },
        )

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `skal opprette avklaringsbehov for vurderingsbehov som tvinger avklaringsbehov`() {
        val sak = opprettInMemorySak()
        val behandlingId = BehandlingId(2008)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM

        val startDato = LocalDate.of(2024, 6, 1)
        val periode1 = Periode(startDato, startDato.plusMonths(1).minusDays(1))
        val helePeriode = Periode(startDato, startDato.plusMonths(2))

        val nårVurderingErRelevant: (FlytKontekstMedPerioder) -> Tidslinje<Boolean> = {
            Tidslinje(
                listOf(
                    Segment(periode1, true),
                )
            )
        }
        val perioderSomIkkeErTilstrekkeligVurdert = emptySet<Periode>()

        val kontekst = flytKontekstMedPerioder {
            this.sakId = sak.id
            this.behandlingId = behandlingId
            this.rettighetsperiode = helePeriode
            this.vurderingsbehovRelevanteForSteg = setOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)
        }

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
            definisjon = definisjon,
            tvingerAvklaringsbehov = setOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
            nårVurderingErRelevant = nårVurderingErRelevant,
            kontekst = kontekst,
            perioderSomIkkeErTilstrekkeligVurdert = { perioderSomIkkeErTilstrekkeligVurdert },
            tilbakestillGrunnlag = { error("skal ikke tilbakestilles") },
        )

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.OPPRETTET)
    }

    @Test
    fun `skal opprette avklaringsbehov for vurderingsbehov som tvinger avklaringsbehov, også når behovet har blitt løst før, men kun om vurderingsbehovet er nyere`() {
        val sak = opprettInMemorySak()
        val behandlingId = BehandlingId(20099)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM
        avklaringsbehovene.leggTil(definisjon, definisjon.løsesISteg, null, null)
        avklaringsbehovene.løsAvklaringsbehov(definisjon, begrunnelse = "Test", endretAv = "Tester")

        val startDato = LocalDate.of(2024, 6, 1)
        val periode1 = Periode(startDato, startDato.plusMonths(1).minusDays(1))
        val helePeriode = Periode(startDato, startDato.plusMonths(2))

        val nårVurderingErRelevant: (FlytKontekstMedPerioder) -> Tidslinje<Boolean> = {
            Tidslinje(
                listOf(
                    Segment(periode1, true),
                )
            )
        }
        val perioderSomIkkeErTilstrekkeligVurdert = emptySet<Periode>()

        val kontekst = flytKontekstMedPerioder {
            this.sakId = sak.id
            this.behandlingId = behandlingId
            this.rettighetsperiode = helePeriode
            this.vurderingsbehovRelevanteForStegMedPerioder =
                setOf(
                    VurderingsbehovMedPeriode(
                        Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND,
                        oppdatertTid = LocalDateTime.now().plusMinutes(1)
                    )
                )
        }

        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
            definisjon = definisjon,
            tvingerAvklaringsbehov = setOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
            nårVurderingErRelevant = nårVurderingErRelevant,
            kontekst = kontekst,
            perioderSomIkkeErTilstrekkeligVurdert = { perioderSomIkkeErTilstrekkeligVurdert },
            tilbakestillGrunnlag = { error("skal ikke tilbakestilles") },
        )

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.OPPRETTET)
    }

    @Test
    fun `skal opprette avklaringsbehov i behandling med nytt krav, selv om alle relevante perioder er vurdert tidligere`() {
        val behandlingId = BehandlingId(20100)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val definisjon = Definisjon.AVKLAR_SYKDOM

        val sak = opprettInMemorySak()
        val forrigeBehandling = InMemoryBehandlingRepository.opprettBehandling(
            sakId = sak.id,
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD
            ),
        )
        val forrigeBehandlingId = forrigeBehandling.id


        val startDato = LocalDate.of(2024, 6, 1)
        val helePeriode = Periode(startDato, startDato.plusMonths(2))

        InMemoryVilkårsresultatRepository.lagre(forrigeBehandlingId, genererVilkårsresultat(helePeriode))

        val førsteKrav = opprettNyttKrav(forrigeBehandlingId, startDato)
        InMemoryKravRepository.lagre(forrigeBehandlingId, setOf(førsteKrav))

        val andreKrav = opprettNyttKrav(forrigeBehandlingId, startDato.plusMonths(1).plusWeeks(2))
        InMemoryKravRepository.lagre(behandlingId, setOf(førsteKrav, andreKrav))

        val nårVurderingErRelevant: (FlytKontekstMedPerioder) -> Tidslinje<Boolean> = {
            Tidslinje(
                listOf(
                    Segment(helePeriode, true),
                )
            )
        }
        val perioderSomIkkeErTilstrekkeligVurdert = emptySet<Periode>()

        val kontekst = flytKontekstMedPerioder {
            this.behandlingId = behandlingId
            this.forrigeBehandlingId = forrigeBehandlingId
            this.sakId = sak.id
            this.rettighetsperiode = helePeriode
            this.vurderingsbehovRelevanteForStegMedPerioder = emptySet()
        }

        avklaringsbehovServiceMedKrav.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkårTilstrekkeligVurdert(
            definisjon = definisjon,
            tvingerAvklaringsbehov = setOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
            nårVurderingErRelevant = nårVurderingErRelevant,
            kontekst = kontekst,
            perioderSomIkkeErTilstrekkeligVurdert = { perioderSomIkkeErTilstrekkeligVurdert },
            tilbakestillGrunnlag = { error("skal ikke tilbakestilles") },
        )

        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.OPPRETTET)
        assertThat(avklaringsbehov?.perioderVedtaketBehøverVurdering).containsExactlyInAnyOrder(
            Periode(
                andreKrav.muligRettFra,
                helePeriode.tom
            )
        )
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
        val kontekst = flytKontekstMedPerioder {
            this.behandlingId = behandlingId
            this.rettighetsperiode = Periode(LocalDate.now(), Tid.MAKS)
        }

        // Act
        avklaringsbehovService.oppdaterAvklaringsbehov(
            definisjon = definisjon,
            vedtakBehøverVurdering = vedtakBehøverVurdering,
            erTilstrekkeligVurdert = erTilstrekkeligVurdert,
            tilbakestillGrunnlag = { error("skal ikke tilbakestilles") },
            kontekst = kontekst
        )

        // Assert
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.AVSLUTTET)
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
        var erTilbakestilt = false
        val kontekst = flytKontekstMedPerioder {
            this.behandlingId = behandlingId
            this.rettighetsperiode = Periode(LocalDate.now(), Tid.MAKS)
        }

        trukketSøknadRepository.lagreTrukketSøknadVurdering(
            behandlingId, TrukketSøknadVurdering(
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
            tilbakestillGrunnlag = { erTilbakestilt = true },
            kontekst = kontekst
        )

        // Assert
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(definisjon)
        assertThat(avklaringsbehov?.status()).isEqualTo(Status.AVBRUTT)
        assertThat(erTilbakestilt).isTrue
    }

    @Test
    fun `løfter avbrutt avklaringsbehov hvis det blir relevant igjen`() {
        val sak = opprettInMemorySak()
        val rettighetsperiode = Periode(LocalDate.now(), Tid.MAKS)
        val behandlingId = BehandlingId(1004)
        val avklaringsbehovene = Avklaringsbehovene(avklaringsbehovRepository, behandlingId)
        val kontekst = flytKontekstMedPerioder {
            sakId = sak.id
            this.behandlingId = behandlingId
            this.rettighetsperiode = rettighetsperiode
            behandlingType = TypeBehandling.Førstegangsbehandling
        }

        // Løfter avklaringsbehov når det er relevant å vurdere bistandsbehov
        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = AVKLAR_BISTANDSBEHOV,
            tvingerAvklaringsbehov = setOf(),
            nårVurderingErRelevant = {
                if (kontekst.behandlingId == behandlingId) {
                    tidslinjeOf(rettighetsperiode to true)
                } else {
                    tidslinjeOf()
                }
            },
            nårVurderingErGyldig = { Tidslinje() },
            tilbakestillGrunnlag = { },
            kontekst = kontekst
        )
        avklaringsbehovene.hentBehovForDefinisjon(AVKLAR_BISTANDSBEHOV).also {
            assertThat(it?.status()).isEqualTo(Status.OPPRETTET)
        }

        // Avbryter avklaringsbehovet hvis bistandsbehov ikke er relevant
        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = AVKLAR_BISTANDSBEHOV,
            tvingerAvklaringsbehov = setOf(),
            nårVurderingErRelevant = { tidslinjeOf() },
            nårVurderingErGyldig = { Tidslinje() },
            tilbakestillGrunnlag = { },
            kontekst = kontekst
        )
        avklaringsbehovene.hentBehovForDefinisjon(AVKLAR_BISTANDSBEHOV).also {
            assertThat(it?.status()).isEqualTo(Status.AVBRUTT)
        }

        // Relevansen for bistandsbehov er tilbake (i samme behandling), og avklaringsbehov løftes
        avklaringsbehovService.oppdaterAvklaringsbehovForPeriodisertYtelsesvilkår(
            definisjon = AVKLAR_BISTANDSBEHOV,
            tvingerAvklaringsbehov = setOf(),
            nårVurderingErRelevant = {
                if (kontekst.behandlingId == behandlingId) {
                    tidslinjeOf(rettighetsperiode to true)
                } else {
                    tidslinjeOf()
                }
            },
            nårVurderingErGyldig = { Tidslinje() },
            tilbakestillGrunnlag = { },
            kontekst = kontekst
        )

        avklaringsbehovene.hentBehovForDefinisjon(AVKLAR_BISTANDSBEHOV).also {
            assertThat(it?.status()).isEqualTo(Status.OPPRETTET)
        }
    }

    private fun opprettNyttKrav(behandlingId: BehandlingId, kravdato: LocalDate): NyttKrav {
        return NyttKrav(
            referanse = Kravreferanse.ny(),
            journalpostId = JournalpostId("JP-001"),
            vurdertAv = Bruker("Z123456"),
            begrunnelse = "Standard krav om AAP",
            vurdertIBehandling = behandlingId,
            opprettet = Instant.now(),
            søknadsdato = Søknadsdato(kravdato, SøknadsdatoÅrsak.SøknadMottatt),
            overstyrMuligRettFra = null,
            muligRettFra = kravdato,
        )
    }
}

