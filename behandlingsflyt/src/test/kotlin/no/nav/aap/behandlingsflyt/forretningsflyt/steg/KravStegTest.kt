package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovService
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertRettighetstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFra
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFraÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Tilleggsopplysning
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.FeatureToggle
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class KravStegTest {

    private lateinit var behandlingService: BehandlingService
    private lateinit var steg: KravSteg

    @BeforeEach
    fun setup() {
        behandlingService = mockk()
        steg = KravSteg(
            unleashGateway = KravAutomatiskVurderingUnleash,
            kravRepository = InMemoryKravRepository,
            mottattDokumentRepository = InMemoryMottattDokumentRepository,
            avklaringsbehovService = mockk(relaxed = true),
            sakRepository = InMemorySakRepository,
            behandlingService = behandlingService,
        )
    }

    @Test
    fun `toggle KravAutomatiskVurdering av - grunnlag forblir uendret`() {
        val stegUtenAutomatisk = KravSteg(
            unleashGateway = AlleAvskruddUnleash,
            kravRepository = InMemoryKravRepository,
            mottattDokumentRepository = InMemoryMottattDokumentRepository,
            avklaringsbehovService = mockk(relaxed = true),
            sakRepository = InMemorySakRepository,
            behandlingService = behandlingService,
        )
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)

        val resultat = stegUtenAutomatisk.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        assertThat(resultat).isEqualTo(Fullført)
        assertThat(InMemoryKravRepository.hentHvisEksisterer(behandling.id)).isNull()
    }

    @Test
    fun `migrering fra Arena - hopper over automatisk krav-vurdering selv med søknad i behandlingen`() {
        val stegMedManuellVurdering = KravSteg(
            unleashGateway = KravManuellVurderingUnleash,
            kravRepository = InMemoryKravRepository,
            mottattDokumentRepository = InMemoryMottattDokumentRepository,
            avklaringsbehovService = mockk(relaxed = true),
            sakRepository = InMemorySakRepository,
            behandlingService = behandlingService,
        )
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        leggTilSøknad(behandling.id, sak.id, LocalDate.of(2024, 1, 15).atStartOfDay())

        stegMedManuellVurdering.utfør(flytKontekstMedPerioder {
            this.behandling = behandling
            this.vurderingType = VurderingType.MIGERING_FRA_ARENA
        })

        assertThat(InMemoryKravRepository.hentHvisEksisterer(behandling.id)).isNull()
    }

    @Test
    fun `migrering fra Arena - vedtakBehøverVurdering og erTilstrekkeligVurdert reflekterer krav-grunnlaget`() {
        val avklaringsbehovService = mockk<AvklaringsbehovService>(relaxed = true)
        val stegMedManuellVurdering = KravSteg(
            unleashGateway = KravManuellVurderingUnleash,
            kravRepository = InMemoryKravRepository,
            mottattDokumentRepository = InMemoryMottattDokumentRepository,
            avklaringsbehovService = avklaringsbehovService,
            sakRepository = InMemorySakRepository,
            behandlingService = behandlingService,
        )
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)

        val vedtakBehøverVurdering = slot<() -> Boolean>()
        val erTilstrekkeligVurdert = slot<() -> Boolean>()
        every {
            avklaringsbehovService.oppdaterAvklaringsbehov(
                definisjon = any(),
                vedtakBehøverVurdering = capture(vedtakBehøverVurdering),
                erTilstrekkeligVurdert = capture(erTilstrekkeligVurdert),
                tilbakestillGrunnlag = any(),
                kontekst = any(),
            )
        } returns Unit

        val kontekst = flytKontekstMedPerioder {
            this.behandling = behandling
            this.vurderingType = VurderingType.MIGERING_FRA_ARENA
        }

        stegMedManuellVurdering.utfør(kontekst)
        assertThat(vedtakBehøverVurdering.captured()).isTrue()
        assertThat(erTilstrekkeligVurdert.captured()).isFalse()

        val migrertKrav = MigrertKrav(
            referanse = Kravreferanse.ny(),
            vurdertAv = SYSTEMBRUKER,
            begrunnelse = "Migrert fra Arena",
            vurdertIBehandling = behandling.id,
            opprettet = Instant.now(),
            virkningstidspunktArena = LocalDate.of(2020, 1, 1),
            muligRettFra = LocalDate.of(2020, 1, 1),
            arenaSaksnummer = "ARENA-1",
            rettighetstype = MigrertRettighetstype.ORDINÆR,
            resterendeKvoteOrdinaer = 0,
        )
        InMemoryKravRepository.lagre(behandling.id, setOf(migrertKrav))

        stegMedManuellVurdering.utfør(kontekst)
        assertThat(erTilstrekkeligVurdert.captured()).isTrue()
    }

    @Test
    fun `førstegangsbehandling med én søknad - lagrer ett NyttKrav med riktig dato`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val søknadsdato = LocalDate.of(2024, 1, 15)
        leggTilSøknad(behandling.id, sak.id, søknadsdato.atStartOfDay())

        every { behandlingService.utledFaktiskBehandlingstype(behandling.id) } returns TypeBehandling.Førstegangsbehandling

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryKravRepository.hent(behandling.id).vurderinger
        assertThat(vurderinger).hasSize(1)
        val krav = vurderinger.single()
        assertThat(krav).isInstanceOf(RelevantKrav::class.java)
        assertThat((krav as RelevantKrav).muligRettFra).isEqualTo(søknadsdato)
        assertThat(krav.overstyrMuligRettFra).isNull()
    }

    @Test
    fun `førstegangsbehandling med tre søknader - ett NyttKrav for eldste og to Tilleggsopplysning`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val eldsteDato = LocalDate.of(2024, 1, 10)
        leggTilSøknad(behandling.id, sak.id, eldsteDato.atStartOfDay())
        leggTilSøknad(behandling.id, sak.id, LocalDate.of(2024, 2, 1).atStartOfDay())
        leggTilSøknad(behandling.id, sak.id, LocalDate.of(2024, 3, 1).atStartOfDay())

        every { behandlingService.utledFaktiskBehandlingstype(behandling.id) } returns TypeBehandling.Førstegangsbehandling

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryKravRepository.hent(behandling.id).vurderinger
        assertThat(vurderinger.filterIsInstance<RelevantKrav>()).hasSize(1)
        assertThat(vurderinger.filterIsInstance<Tilleggsopplysning>()).hasSize(2)
        assertThat(vurderinger.filterIsInstance<RelevantKrav>().single().muligRettFra).isEqualTo(eldsteDato)
    }

    @Test
    fun `revurdering uten rent avslag med én søknad - lagrer Tilleggsopplysning`() {
        val sak = opprettInMemorySak()
        val forrigeBehandling = opprettFørstegangsbehandling(sak.id)
        val behandling = opprettRevurdering(sak.id, forrigeBehandling.id)
        leggTilSøknad(forrigeBehandling.id, sak.id, LocalDate.of(2024, 5, 1).atStartOfDay())
        leggTilSøknad(behandling.id, sak.id, LocalDate.of(2025, 5, 1).atStartOfDay())

        every { behandlingService.utledFaktiskBehandlingstype(forrigeBehandling.id) } returns TypeBehandling.Førstegangsbehandling
        every { behandlingService.utledFaktiskBehandlingstype(behandling.id) } returns TypeBehandling.Revurdering
        
        steg.utfør(flytKontekstMedPerioder { this.behandling = forrigeBehandling })
        val vurderingerFørstegangsbehandling = InMemoryKravRepository.hent(forrigeBehandling.id).vurderinger
        assertThat(vurderingerFørstegangsbehandling).hasSize(1)
        assertThat(vurderingerFørstegangsbehandling.single()).isInstanceOf(RelevantKrav::class.java)

        // Simulerer GrunnlagKopierer.kopier som kjøres ved opprettelse av revurdering
        InMemoryKravRepository.kopier(forrigeBehandling.id, behandling.id)

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryKravRepository.hent(behandling.id).vurderinger
        assertThat(vurderinger).hasSize(2)
        assertThat(vurderinger.filterIsInstance<RelevantKrav>()).hasSize(1)
        assertThat(vurderinger.filterIsInstance<RelevantKrav>().first().vurdertIBehandling).isEqualTo(forrigeBehandling.id)
        assertThat(vurderinger.filterIsInstance<Tilleggsopplysning>()).hasSize(1)
        assertThat(vurderinger.filterIsInstance<Tilleggsopplysning>().first().vurdertIBehandling).isEqualTo(behandling.id)
    }

    @Test
    @Disabled("Starte med kun ett relevant krav")
    fun `revurdering etter rent avslag bevarer NyttKrav fra forrige behandling og oppretter nytt`() {
        val sak = opprettInMemorySak()
        val forrigeBehandling = opprettFørstegangsbehandling(sak.id)
        val behandling = opprettRevurdering(sak.id, forrigeBehandling.id)

        val gammeltNyttKrav = lagNyttKrav(forrigeBehandling.id, JournalpostId("JP001"), LocalDate.of(2023, 1, 1))
        InMemoryKravRepository.lagre(forrigeBehandling.id, setOf(gammeltNyttKrav))

        // Simulerer GrunnlagKopierer.kopier som kjøres ved opprettelse av revurdering
        InMemoryKravRepository.kopier(forrigeBehandling.id, behandling.id)

        leggTilSøknad(behandling.id, sak.id, LocalDate.of(2024, 1, 1).atStartOfDay())
        leggTilSøknad(behandling.id, sak.id, LocalDate.of(2024, 2, 1).atStartOfDay())

        every { behandlingService.utledFaktiskBehandlingstype(behandling.id) } returns TypeBehandling.Førstegangsbehandling

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryKravRepository.hent(behandling.id).vurderinger
        assertThat(vurderinger.filter{it.vurdertIBehandling == forrigeBehandling.id}).hasSize(1)
        assertThat(vurderinger.filter{it.vurdertIBehandling == behandling.id}).hasSize(2)
        assertThat(vurderinger.filterIsInstance<RelevantKrav>()).hasSize(2)
        assertThat(vurderinger.filterIsInstance<Tilleggsopplysning>()).hasSize(1)
        assertThat(vurderinger.filterIsInstance<RelevantKrav>().map { it.referanse }).contains(gammeltNyttKrav.referanse)
    }

    @Test
    fun `overstyrt krav i inneværende behandling – overstyring kopieres til nytt RelevantKrav fra søknad`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val overstyrtDato = LocalDate.of(2023, 6, 1)

        val overstyrtKrav = lagNyttKrav(
            behandlingId = behandling.id,
            journalpostId = JournalpostId("JP_OVERSTYRT"),
            mottattDato = LocalDate.of(2024, 1, 1),
            overstyrMuligRettFra = OverstyrMuligRettFra(
                dato = overstyrtDato,
                årsak = OverstyrMuligRettFraÅrsak.IkkeIStandTilÅSøkeTidligere,
                begrunnelse = "Overstyrt",
            ),
        )
        InMemoryKravRepository.lagre(behandling.id, setOf(overstyrtKrav))

        leggTilSøknad(behandling.id, sak.id, LocalDate.of(2024, 3, 1).atStartOfDay())

        every { behandlingService.utledFaktiskBehandlingstype(behandling.id) } returns TypeBehandling.Førstegangsbehandling

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryKravRepository.hent(behandling.id).vurderinger
        val relevanteKrav = vurderinger.filterIsInstance<RelevantKrav>()
        assertThat(relevanteKrav).hasSize(1)
        // Ny RelevantKrav fra søknad – ikke den gamle manuelle
        assertThat(relevanteKrav.single().referanse).isNotEqualTo(overstyrtKrav.referanse)
        // Overstyringen er kopiert
        assertThat(relevanteKrav.single().overstyrMuligRettFra?.dato).isEqualTo(overstyrtDato)
        assertThat(relevanteKrav.single().muligRettFra).isEqualTo(overstyrtDato)
        assertThat(vurderinger.filterIsInstance<Tilleggsopplysning>()).isEmpty()
    }

    @Test
    @Disabled("Starter med kun ett relevant krav")
    fun `søknad etter rent avslag skal lagres som NyttKrav selv om det finnes et overstyrt krav fra forrige behandling`() {
        val sak = opprettInMemorySak()
        val forrigeBehandling = opprettFørstegangsbehandling(sak.id)
        val behandling = opprettRevurdering(sak.id, forrigeBehandling.id)

        val overstyrtKravFraForrige = lagNyttKrav(
            behandlingId = forrigeBehandling.id,
            journalpostId = JournalpostId("JP_FORRIGE"),
            mottattDato = LocalDate.of(2023, 1, 1),
            overstyrMuligRettFra = OverstyrMuligRettFra(
                dato = LocalDate.of(2022, 6, 1),
                årsak = OverstyrMuligRettFraÅrsak.MisvisendeOpplysninger,
                begrunnelse = "Overstyrt",
            ),
        )
        InMemoryKravRepository.lagre(forrigeBehandling.id, setOf(overstyrtKravFraForrige))

        // Simulerer GrunnlagKopierer.kopier som kjøres ved opprettelse av revurdering
        InMemoryKravRepository.kopier(forrigeBehandling.id, behandling.id)

        leggTilSøknad(behandling.id, sak.id, LocalDate.of(2024, 1, 1).atStartOfDay())

        every { behandlingService.utledFaktiskBehandlingstype(behandling.id) } returns TypeBehandling.Førstegangsbehandling

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryKravRepository.hent(behandling.id).vurderinger
        val relevantKravIInneværende = vurderinger.filterIsInstance<RelevantKrav>()
            .filter { it.vurdertIBehandling == behandling.id }
        assertThat(relevantKravIInneværende).hasSize(1)
        assertThat(vurderinger.filterIsInstance<Tilleggsopplysning>()).isEmpty()
    }

    @Test
    fun `re-kjøring av steget gir stabilt grunnlag uten duplikater`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        leggTilSøknad(behandling.id, sak.id, LocalDate.of(2024, 1, 1).atStartOfDay())

        every { behandlingService.utledFaktiskBehandlingstype(behandling.id) } returns TypeBehandling.Førstegangsbehandling

        val kontekst = flytKontekstMedPerioder { this.behandling = behandling }
        steg.utfør(kontekst)
        steg.utfør(kontekst)

        val vurderinger = InMemoryKravRepository.hent(behandling.id).vurderinger
        assertThat(vurderinger).hasSize(1)
        assertThat(vurderinger.filterIsInstance<RelevantKrav>()).hasSize(1)
    }

    @Test
    fun `historisk revurdering uten migrert grunnlag skippes – BackfillKrav håndterer den`() {
        val sak = opprettInMemorySak()
        val forrigeBehandling = opprettFørstegangsbehandling(sak.id)
        val behandling = opprettRevurdering(sak.id, forrigeBehandling.id)
        leggTilSøknad(behandling.id, sak.id, LocalDate.of(2024, 5, 1).atStartOfDay())

        every { behandlingService.utledFaktiskBehandlingstype(behandling.id) } returns TypeBehandling.Revurdering

        // Ingen kopier-kall – simulerer at forrige behandling ikke er migrert ennå
        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        assertThat(InMemoryKravRepository.hentHvisEksisterer(behandling.id)).isNull()
    }

    @Test
    fun `legeerklæring som eneste dokument på første behandling – lagres som RelevantKrav`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val legeerklæringDato = LocalDate.of(2024, 1, 1)
        leggTilLegeerklæring(behandling.id, sak.id, legeerklæringDato.atStartOfDay())

        every { behandlingService.utledFaktiskBehandlingstype(behandling.id) } returns TypeBehandling.Førstegangsbehandling

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryKravRepository.hent(behandling.id).vurderinger
        assertThat(vurderinger.filterIsInstance<RelevantKrav>()).hasSize(1)
        assertThat(vurderinger.filterIsInstance<RelevantKrav>().single().muligRettFra).isEqualTo(legeerklæringDato)
    }

    @Test
    fun `legeerklæring før søknad – legeerklæring er krav, søknad er tilleggsopplysning`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val legeerklæringDato = LocalDate.of(2024, 1, 1)
        val søknadDato = LocalDate.of(2024, 2, 1)
        leggTilLegeerklæring(behandling.id, sak.id, legeerklæringDato.atStartOfDay())
        leggTilSøknad(behandling.id, sak.id, søknadDato.atStartOfDay())

        every { behandlingService.utledFaktiskBehandlingstype(behandling.id) } returns TypeBehandling.Førstegangsbehandling

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryKravRepository.hent(behandling.id).vurderinger
        assertThat(vurderinger.filterIsInstance<RelevantKrav>()).hasSize(1)
        assertThat(vurderinger.filterIsInstance<RelevantKrav>().single().muligRettFra).isEqualTo(legeerklæringDato)
        assertThat(vurderinger.filterIsInstance<Tilleggsopplysning>()).hasSize(1)
    }

    @Test
    fun `søknad før legeerklæring – søknad er krav, legeerklæring ignoreres`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val søknadDato = LocalDate.of(2024, 1, 1)
        val legeerklæringDato = LocalDate.of(2024, 2, 1)
        leggTilSøknad(behandling.id, sak.id, søknadDato.atStartOfDay())
        leggTilLegeerklæring(behandling.id, sak.id, legeerklæringDato.atStartOfDay())

        every { behandlingService.utledFaktiskBehandlingstype(behandling.id) } returns TypeBehandling.Førstegangsbehandling

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryKravRepository.hent(behandling.id).vurderinger
        assertThat(vurderinger.filterIsInstance<RelevantKrav>()).hasSize(1)
        assertThat(vurderinger.filterIsInstance<RelevantKrav>().single().muligRettFra).isEqualTo(søknadDato)
        assertThat(vurderinger.filterIsInstance<Tilleggsopplysning>()).isEmpty()
    }

    @Test
    fun `legeerklæring erstattes av søknad som kom inn tidligere – re-kjøring av steg`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val legeerklæringDato = LocalDate.of(2024, 2, 1)
        val søknadDato = LocalDate.of(2024, 1, 1) // søknad er eldre

        every { behandlingService.utledFaktiskBehandlingstype(behandling.id) } returns TypeBehandling.Førstegangsbehandling

        // Første kjøring: bare legeerklæring
        leggTilLegeerklæring(behandling.id, sak.id, legeerklæringDato.atStartOfDay())
        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })
        assertThat(InMemoryKravRepository.hent(behandling.id).vurderinger.filterIsInstance<RelevantKrav>().single().muligRettFra)
            .isEqualTo(legeerklæringDato)

        // Andre kjøring: søknad kommer inn med tidligere dato
        leggTilSøknad(behandling.id, sak.id, søknadDato.atStartOfDay())
        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryKravRepository.hent(behandling.id).vurderinger
        assertThat(vurderinger.filterIsInstance<RelevantKrav>()).hasSize(1)
        assertThat(vurderinger.filterIsInstance<RelevantKrav>().single().muligRettFra).isEqualTo(søknadDato)
        assertThat(vurderinger.filterIsInstance<Tilleggsopplysning>()).isEmpty()
    }

    @Test
    fun `revurdering med søknad eldre enn vedtatt krav – søknad tar over som RelevantKrav, gammelt krav nedgraderes`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val revurdering = opprettRevurdering(sak.id, behandling.id)

        val gammeltSøknadDato = LocalDate.of(2024, 2, 1)
        val nyttSøknadDato = LocalDate.of(2024, 1, 1) // eldre

        every { behandlingService.utledFaktiskBehandlingstype(behandling.id) } returns TypeBehandling.Førstegangsbehandling
        every { behandlingService.utledFaktiskBehandlingstype(revurdering.id) } returns TypeBehandling.Revurdering

        leggTilSøknad(behandling.id, sak.id, gammeltSøknadDato.atStartOfDay())
        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val gammeltKrav = InMemoryKravRepository.hent(behandling.id).gjeldendeRelevanteKrav().single()
        InMemoryKravRepository.kopier(behandling.id, revurdering.id)

        leggTilSøknad(revurdering.id, sak.id, nyttSøknadDato.atStartOfDay())
        steg.utfør(flytKontekstMedPerioder { this.behandling = revurdering })

        val gjeldendeVurderinger = InMemoryKravRepository.hent(revurdering.id).gjeldendeVurderinger()
        val relevanteKrav = gjeldendeVurderinger.filterIsInstance<RelevantKrav>()
        val tilleggsopplysninger = gjeldendeVurderinger.filterIsInstance<Tilleggsopplysning>()

        assertThat(relevanteKrav).hasSize(1)
        assertThat(relevanteKrav.single().muligRettFra).isEqualTo(nyttSøknadDato)
        assertThat(tilleggsopplysninger).hasSize(1)
        assertThat(tilleggsopplysninger.single().referanse).isEqualTo(gammeltKrav.referanse)
    }

    @Test
    fun `Tilbakedatert søknad i revurdering arver overstyrMuligRettFra fra vedtatt krav`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val revurdering = opprettRevurdering(sak.id, behandling.id)

        val gammeltSøknadDato = LocalDate.of(2024, 2, 1)
        val overstyrtDato = LocalDate.of(2023, 6, 1)
        val nyttSøknadDato = LocalDate.of(2024, 1, 15) // eldre enn søknadsdato, men nyere enn overstyrtDato

        every { behandlingService.utledFaktiskBehandlingstype(behandling.id) } returns TypeBehandling.Førstegangsbehandling
        every { behandlingService.utledFaktiskBehandlingstype(revurdering.id) } returns TypeBehandling.Revurdering

        leggTilSøknad(behandling.id, sak.id, gammeltSøknadDato.atStartOfDay())
        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val gammeltKrav = InMemoryKravRepository.hent(behandling.id).gjeldendeRelevanteKrav().single()
        InMemoryKravRepository.lagre(
            behandling.id,
            InMemoryKravRepository.hent(behandling.id).vurderinger
                .map { if (it.referanse == gammeltKrav.referanse) gammeltKrav.copy(
                    overstyrMuligRettFra = OverstyrMuligRettFra(overstyrtDato, OverstyrMuligRettFraÅrsak.IkkeIStandTilÅSøkeTidligere, "Min begrunnelse"),
                    muligRettFra = overstyrtDato,
                ) else it }
                .toSet()
        )
        InMemoryKravRepository.kopier(behandling.id, revurdering.id)

        leggTilSøknad(revurdering.id, sak.id, nyttSøknadDato.atStartOfDay())
        steg.utfør(flytKontekstMedPerioder { this.behandling = revurdering })

        val nyttKrav = InMemoryKravRepository.hent(revurdering.id).gjeldendeRelevanteKrav().single()
        assertThat(nyttKrav.søknadsdato.dato).isEqualTo(nyttSøknadDato)
        assertThat(nyttKrav.overstyrMuligRettFra).isNotNull()
        assertThat(nyttKrav.overstyrMuligRettFra!!.dato).isEqualTo(overstyrtDato)
        assertThat(nyttKrav.muligRettFra).isEqualTo(overstyrtDato) // min(15 jan 2024, 1 jun 2023)
    }

    private fun opprettFørstegangsbehandling(sakId: SakId) =
        InMemoryBehandlingRepository.opprettBehandling(
            sakId = sakId,
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            forrigeBehandlingId = null,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
            ),
        )

    private fun opprettRevurdering(sakId: SakId, forrigeBehandlingId: BehandlingId) =
        InMemoryBehandlingRepository.opprettBehandling(
            sakId = sakId,
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = forrigeBehandlingId,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = listOf(VurderingsbehovMedPeriode(Vurderingsbehov.MOTTATT_SØKNAD)),
                årsak = ÅrsakTilOpprettelse.SØKNAD,
            ),
        )

    private fun leggTilSøknad(behandlingId: BehandlingId, sakId: SakId, mottattTidspunkt: LocalDateTime) {
        InMemoryMottattDokumentRepository.lagre(
            MottattDokument(
                referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, UUID.randomUUID().toString()),
                sakId = sakId,
                behandlingId = behandlingId,
                mottattTidspunkt = mottattTidspunkt,
                type = InnsendingType.SØKNAD,
                kanal = Kanal.DIGITAL,
                strukturertDokument = null,
            )
        )
    }

    private fun leggTilLegeerklæring(behandlingId: BehandlingId, sakId: SakId, mottattTidspunkt: LocalDateTime) {
        InMemoryMottattDokumentRepository.lagre(
            MottattDokument(
                referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, UUID.randomUUID().toString()),
                sakId = sakId,
                behandlingId = behandlingId,
                mottattTidspunkt = mottattTidspunkt,
                type = InnsendingType.LEGEERKLÆRING,
                kanal = Kanal.DIGITAL,
                strukturertDokument = null,
            )
        )
    }

    private fun lagNyttKrav(
        behandlingId: BehandlingId,
        journalpostId: JournalpostId,
        mottattDato: LocalDate,
        overstyrMuligRettFra: OverstyrMuligRettFra? = null,
    ) = RelevantKrav(
        referanse = Kravreferanse.ny(),
        journalpostId = journalpostId,
        vurdertAv = SYSTEMBRUKER,
        begrunnelse = "Test",
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
        søknadsdato = Søknadsdato(mottattDato, SøknadsdatoÅrsak.SøknadMottatt, begrunnelse = "Test"),
        overstyrMuligRettFra = overstyrMuligRettFra,
        muligRettFra = overstyrMuligRettFra?.dato ?: mottattDato,
    )

    private object KravAutomatiskVurderingUnleash : FakeUnleashBaseWithDefaultDisabled(
        listOf(
            BehandlingsflytFeature.KravSteg,
            BehandlingsflytFeature.KravAutomatiskVurdering,
        )
    )

    private object AlleAvskruddUnleash : FakeUnleashBaseWithDefaultDisabled(emptyList())

    /**
     * Unleash-fake som simulerer at manuell vurdering av krav er skrudd på for alle saker,
     * slik at KravSteg tar veien via when(vurderingType)-grenen (inkl. migrering fra Arena).
     */
    private object KravManuellVurderingUnleash : FakeUnleashBaseWithDefaultDisabled(
        listOf(BehandlingsflytFeature.KravSteg)
    ) {
        override fun erPåskruddForSak(featureToggle: FeatureToggle, variantName: String, saksnummer: Saksnummer) =
            featureToggle == BehandlingsflytFeature.KravManuellVurdering
    }
}
