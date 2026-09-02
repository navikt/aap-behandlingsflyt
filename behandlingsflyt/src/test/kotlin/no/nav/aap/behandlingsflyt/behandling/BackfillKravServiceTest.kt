package no.nav.aap.behandlingsflyt.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.AarsakTilTrekkSoknad
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFra
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFraÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Tilleggsopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeHarRett
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgBehandling
import no.nav.aap.behandlingsflyt.help.opprettInMemorySakOgRevurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryStønadsperiodeRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class BackfillKravServiceTest {

    private lateinit var rettighetsperiodeRepository: VurderRettighetsperiodeRepository
    private lateinit var service: BackfillKravService

    @BeforeEach
    fun setup() {
        InMemoryStønadsperiodeRepository.reset()
        rettighetsperiodeRepository = mockk {
            every { hentVurdering(any()) } returns null
        }
        service = BackfillKravService(
            kravRepository = InMemoryKravRepository,
            stønadsperiodeRepository = InMemoryStønadsperiodeRepository,
            mottattDokumentRepository = InMemoryMottattDokumentRepository,
            rettighetsperiodeRepository = rettighetsperiodeRepository,
            trukketSøknadService = TrukketSøknadService(InMemoryTrukketSøknadRepository),
        )
    }

    @Test
    fun `første søknad i sak gir RelevantKrav med riktig dato`() {
        val søknadsdato = 10 januar 2024
        val (sak, behandling) = opprettSakOgBehandlingMedSøknad(søknadsdato)

        service.backfillBehandling(sak, behandling, erNyesteBehandling = true)

        val krav = InMemoryKravRepository.hent(behandling.id)
        val relevantKrav = assertHarNøyaktigEttRelevantKrav(krav.vurderinger)
        assertThat(relevantKrav.muligRettFra).isEqualTo(søknadsdato)
        assertThat(relevantKrav.søknadsdato.dato).isEqualTo(søknadsdato)
        assertThat(relevantKrav.overstyrMuligRettFra).isNull()
    }

    @Test
    fun `påfølgende søknader gir Tilleggsopplysning`() {
        val (sak, behandling) = opprettSakMedToSøknader()

        service.backfillBehandling(sak, behandling, erNyesteBehandling = true)

        val vurderinger = InMemoryKravRepository.hent(behandling.id).gjeldendeVurderinger()
        assertThat(vurderinger.filterIsInstance<RelevantKrav>()).hasSize(1)
        assertThat(vurderinger.filterIsInstance<Tilleggsopplysning>()).hasSize(1)
    }

    @Test
    fun `revurdering uten søknad kopierer krav fra forrige behandling`() {
        val søknadsdato = 5 januar 2024
        val (sak, førstegangsbehandling, revurdering) = opprettInMemorySakOgRevurdering(søknadsdato = søknadsdato)

        leggTilSøknad(førstegangsbehandling, søknadsdato)

        service.backfillBehandling(sak, førstegangsbehandling, erNyesteBehandling = false)
        service.backfillBehandling(sak, revurdering, erNyesteBehandling = true)

        val kravRevurdering = InMemoryKravRepository.hent(revurdering.id)
        assertHarNøyaktigEttRelevantKrav(kravRevurdering.vurderinger)
    }

    @Test
    fun `behandling med eksisterende krav returnerer AlleredeBackfilled`() {
        val søknadsdato = 10 januar 2024
        val (sak, behandling) = opprettSakOgBehandlingMedSøknad(søknadsdato)

        service.backfillBehandling(sak, behandling, erNyesteBehandling = true)

        val resultat = service.backfillBehandling(sak, behandling, erNyesteBehandling = true)

        assertThat(resultat).isEqualTo(BackfillBehandlingResultat.AlleredeBackfilled)
    }

    @Test
    fun `backfill er idempotent – dobbel kjøring gir samme resultat`() {
        val søknadsdato = 10 januar 2024
        val (sak, behandling) = opprettSakOgBehandlingMedSøknad(søknadsdato)

        service.backfillBehandling(sak, behandling, erNyesteBehandling = true)
        val kravFørst = InMemoryKravRepository.hent(behandling.id).gjeldendeVurderinger()

        service.backfillBehandling(sak, behandling, erNyesteBehandling = true)
        val kravAndre = InMemoryKravRepository.hent(behandling.id).gjeldendeVurderinger()

        assertThat(kravFørst.map { it.referanse }).containsExactlyInAnyOrderElementsOf(kravAndre.map { it.referanse })
    }

    // -------------------------------------------------------------------------
    // Trukket søknad
    // -------------------------------------------------------------------------

    @Test
    fun `trukket-søknad-sak hoppes over`() {
        val søknadsdato = 10 januar 2024
        val (sak, behandling) = opprettSakOgBehandlingMedSøknad(søknadsdato)

        InMemoryTrukketSøknadRepository.lagreTrukketSøknadVurdering(
            behandling.id,
            lagTrukketSøknadVurdering(skalTrekkes = true)
        )
        InMemoryBehandlingRepository.oppdaterBehandlingStatus(behandling.id, Status.AVSLUTTET)

        val erTrukket = service.erTrukketSøknadSak(listOf(behandling))

        assertThat(erTrukket).isTrue()
    }

    // -------------------------------------------------------------------------
    // Rettighetsperiodevurdering
    // -------------------------------------------------------------------------

    @Test
    fun `rettighetsperiodevurdering med overstyring setter OverstyrMuligRettFra`() {
        val søknadsdato = 10 januar 2024
        val overstyrtDato = 1 mars 2023
        val (sak, behandling) = opprettSakOgBehandlingMedSøknad(søknadsdato, rettighetsperiodeFom = overstyrtDato)

        every { rettighetsperiodeRepository.hentVurdering(behandling.id) } returns
            lagRettighetsperiodeVurdering(
                harRett = RettighetsperiodeHarRett.HarRettIkkeIStandTilÅSøkeTidligere,
                startDato = overstyrtDato,
            )

        service.backfillBehandling(sak, behandling, erNyesteBehandling = true)

        val krav = assertHarNøyaktigEttRelevantKrav(InMemoryKravRepository.hent(behandling.id).vurderinger)
        assertThat(krav.overstyrMuligRettFra).isNotNull
        assertThat(krav.overstyrMuligRettFra!!.dato).isEqualTo(overstyrtDato)
    }

    @Test
    fun `gjeldende muligRettFra er minimum av mottattdato og overstyrt dato`() {
        val søknadsdato = 10 januar 2024
        val overstyrtDato = 1 mars 2023 // tidligere enn søknad
        val (sak, behandling) = opprettSakOgBehandlingMedSøknad(søknadsdato, rettighetsperiodeFom = overstyrtDato)

        every { rettighetsperiodeRepository.hentVurdering(behandling.id) } returns
            lagRettighetsperiodeVurdering(
                harRett = RettighetsperiodeHarRett.HarRettIkkeIStandTilÅSøkeTidligere,
                startDato = overstyrtDato,
            )

        service.backfillBehandling(sak, behandling, erNyesteBehandling = true)

        val krav = assertHarNøyaktigEttRelevantKrav(InMemoryKravRepository.hent(behandling.id).vurderinger)
        assertThat(krav.muligRettFra).isEqualTo(overstyrtDato)
    }

    @Test
    fun `kræsjer hvis rettighetsperiode fom ikke stemmer med krav muligRettFra`() {
        val søknadsdato = 10 januar 2024
        val feilRettighetsperiodeFom = 15 januar 2024 // avviker fra søknadsdato
        val (sak, behandling) = opprettSakOgBehandlingMedSøknad(
            søknadsdato,
            rettighetsperiodeFom = feilRettighetsperiodeFom
        )

        assertThatThrownBy { service.backfillBehandling(sak, behandling, erNyesteBehandling = true) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("rettighetsperiode.fom")
    }
    
    
    @Test
    fun `stønadsperiode opprettes for hvert relevant krav`() {
        val søknadsdato = 10 januar 2024
        val (sak, behandling) = opprettSakOgBehandlingMedSøknad(søknadsdato)

        service.backfillBehandling(sak, behandling, erNyesteBehandling = true)

        val stønadsperiode = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)
        assertThat(stønadsperiode).isNotNull
        val vurderinger = stønadsperiode!!.vurderinger
        assertThat(vurderinger).hasSize(1)
        assertThat(vurderinger.first().relevantKravType).isEqualTo(RelevantKravType.NY_STØNADSPERIODE)
        assertThat(vurderinger.first().startDato).isEqualTo(søknadsdato)
    }

    @Test
    fun `stønadsperiode-backfill er idempotent`() {
        val søknadsdato = 10 januar 2024
        val (sak, behandling) = opprettSakOgBehandlingMedSøknad(søknadsdato)

        service.backfillBehandling(sak, behandling, erNyesteBehandling = true)
        val antallFørst = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger.size

        service.backfillBehandling(sak, behandling, erNyesteBehandling = true) // second call → AlleredeBackfilled, no change

        val antallAndre = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger.size
        assertThat(antallFørst).isEqualTo(antallAndre)
    }

    @Test
    fun `søknad med tidligere mottattTidspunkt nedgraderer opprinnelig krav til tilleggsopplysning`() {
        val senereSøknadsdato = 10 januar 2024
        val tidligereSøknadsdato = 5 januar 2024
        val (sak, førstegangsbehandling, revurdering) =
            opprettInMemorySakOgRevurdering(søknadsdato = senereSøknadsdato)
        
        leggTilSøknad(førstegangsbehandling, senereSøknadsdato)
        leggTilSøknad(revurdering, tidligereSøknadsdato)
        
        InMemorySakRepository.oppdaterRettighetsperiode(sak.id, Periode(tidligereSøknadsdato, Tid.MAKS)) // Rettighetsperiode ligger på saksnivå
        
        service.backfillBehandling(sak, førstegangsbehandling, erNyesteBehandling = false)
        service.backfillBehandling(sak, revurdering, erNyesteBehandling = true)

        val kravFørstegangsbehandling = InMemoryKravRepository.hent(førstegangsbehandling.id)
        assertThat(kravFørstegangsbehandling.gjeldendeRelevanteKrav()).hasSize(1)
        val relevantKravFørstegangsbehandling = kravFørstegangsbehandling.gjeldendeRelevanteKrav().single()
        assertThat(relevantKravFørstegangsbehandling.muligRettFra).isEqualTo(senereSøknadsdato)
        
        val kravRevurdering = InMemoryKravRepository.hent(revurdering.id)
        val gjeldendeVurderinger = kravRevurdering.gjeldendeVurderinger()

        val relevanteKrav = gjeldendeVurderinger.filterIsInstance<RelevantKrav>()
        val tilleggsopplysninger = gjeldendeVurderinger.filterIsInstance<Tilleggsopplysning>()

        assertThat(relevanteKrav).hasSize(1)
        assertThat(relevanteKrav.single().muligRettFra).isEqualTo(tidligereSøknadsdato)
        assertThat(relevanteKrav.single().referanse).isNotEqualTo(relevantKravFørstegangsbehandling.referanse) 
        assertThat(tilleggsopplysninger).hasSize(1)
        assertThat(tilleggsopplysninger.single().referanse).isEqualTo(relevantKravFørstegangsbehandling.referanse)
    }
    
    @Test
    fun `behandling uten søknad men med legeerklæring bruker legeerklæringen som krav`() {
        val legeerklæringDato = 15 januar 2024
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = legeerklæringDato)
        leggTilLegeerklæring(behandling, legeerklæringDato)

        val sakMedRettighetsperiode = lagSakMedRettighetsperiode(sak, legeerklæringDato)
        service.backfillBehandling(sakMedRettighetsperiode, behandling, erNyesteBehandling = true)

        val krav = InMemoryKravRepository.hent(behandling.id)
        val relevantKrav = assertHarNøyaktigEttRelevantKrav(krav.vurderinger)
        assertThat(relevantKrav.muligRettFra).isEqualTo(legeerklæringDato)
    }

    @Test
    fun `behandling uten søknad og uten legeerklæring kaster feil`() {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = 10 januar 2024)

        val sakMedRettighetsperiode = lagSakMedRettighetsperiode(sak, 10 januar 2024)
        assertThatThrownBy { service.backfillBehandling(sakMedRettighetsperiode, behandling, erNyesteBehandling = true) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Ingen søknad eller legeerklæring")
    }

    @Test
    fun `legeerklæring mottatt før søknad gir muligRettFra basert på legeerklæring`() {
        val legeerklæringDato = 1 januar 2024
        val søknadsdato = 10 januar 2024
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = legeerklæringDato)
        leggTilLegeerklæring(behandling, legeerklæringDato)
        leggTilSøknad(behandling, søknadsdato)

        val sakMedRettighetsperiode = lagSakMedRettighetsperiode(sak, legeerklæringDato)
        service.backfillBehandling(sakMedRettighetsperiode, behandling, erNyesteBehandling = true)

        val krav = assertHarNøyaktigEttRelevantKrav(InMemoryKravRepository.hent(behandling.id).vurderinger)
        assertThat(krav.muligRettFra).isEqualTo(legeerklæringDato)
    }

    @Test
    fun `søknad mottatt før legeerklæring gir muligRettFra basert på søknad`() {
        val søknadsdato = 1 januar 2024
        val legeerklæringDato = 10 januar 2024
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = søknadsdato)
        leggTilSøknad(behandling, søknadsdato)
        leggTilLegeerklæring(behandling, legeerklæringDato)

        val sakMedRettighetsperiode = lagSakMedRettighetsperiode(sak, søknadsdato)
        service.backfillBehandling(sakMedRettighetsperiode, behandling, erNyesteBehandling = true)

        val krav = assertHarNøyaktigEttRelevantKrav(InMemoryKravRepository.hent(behandling.id).vurderinger)
        assertThat(krav.muligRettFra).isEqualTo(søknadsdato)
    }

    @Test
    fun `søknad mottatt før søknadsdato på forrige krav – overtar selv om muligRettFra er overstyrt lavere`() {
        // Gammelt krav: søknadsdato = 10 feb, overstyrt til 1 jan → muligRettFra = 1 jan
        // Ny søknad: 15 jan < 10 feb (søknadsdato) → skal overta (feil med gammel muligRettFra-sammenligning)
        val gammeltSøknadsdato = 10 februar 2024
        val overstyrtDato = 1 januar 2024
        val nySøknadsdato = 15 januar 2024

        val (sak, førstegangsbehandling, revurdering) =
            opprettInMemorySakOgRevurdering(søknadsdato = gammeltSøknadsdato)

        leggTilSøknad(førstegangsbehandling, gammeltSøknadsdato)
        service.backfillBehandling(
            lagSakMedRettighetsperiode(sak, overstyrtDato),
            førstegangsbehandling,
            erNyesteBehandling = false,
        )
        // Sett overstyrMuligRettFra manuelt på det lagrede kravet
        val gammeltKrav = InMemoryKravRepository.hent(førstegangsbehandling.id).gjeldendeRelevanteKrav().single()
        InMemoryKravRepository.lagre(
            førstegangsbehandling.id,
            setOf(gammeltKrav.copy(
                overstyrMuligRettFra = OverstyrMuligRettFra(overstyrtDato, OverstyrMuligRettFraÅrsak.IkkeIStandTilÅSøkeTidligere, begrunnelse = ""),
                muligRettFra = overstyrtDato,
            ))
        )

        leggTilSøknad(revurdering, nySøknadsdato)

        service.backfillBehandling(
            lagSakMedRettighetsperiode(sak, overstyrtDato),
            revurdering,
            erNyesteBehandling = true,
        )

        val gjeldendeVurderinger = InMemoryKravRepository.hent(revurdering.id).gjeldendeVurderinger()
        val relevanteKrav = gjeldendeVurderinger.filterIsInstance<RelevantKrav>()
        assertThat(relevanteKrav).hasSize(1)
        assertThat(relevanteKrav.single().søknadsdato.dato).isEqualTo(nySøknadsdato)
        assertThat(relevanteKrav.single().muligRettFra).isEqualTo(overstyrtDato)
        assertThat(gjeldendeVurderinger.filterIsInstance<Tilleggsopplysning>()).hasSize(1)
    }

    @Test
    fun `overstyrMuligRettFra kopieres til nytt krav som overtar`() {
        val gammeltSøknadsdato = 10 januar 2024
        val overstyrtDato = 1 mars 2023
        val nySøknadsdato = 5 januar 2024 // eldre enn gammelt søknadsdato

        val (sak, førstegangsbehandling, revurdering) =
            opprettInMemorySakOgRevurdering(søknadsdato = gammeltSøknadsdato)

        leggTilSøknad(førstegangsbehandling, gammeltSøknadsdato)
        service.backfillBehandling(
            lagSakMedRettighetsperiode(sak, gammeltSøknadsdato),
            førstegangsbehandling,
            erNyesteBehandling = false,
        )
        val gammeltKrav = InMemoryKravRepository.hent(førstegangsbehandling.id).gjeldendeRelevanteKrav().single()
        InMemoryKravRepository.lagre(
            førstegangsbehandling.id,
            setOf(gammeltKrav.copy(
                overstyrMuligRettFra = OverstyrMuligRettFra(overstyrtDato, OverstyrMuligRettFraÅrsak.IkkeIStandTilÅSøkeTidligere, ""),
                muligRettFra = overstyrtDato,
            ))
        )

        leggTilSøknad(revurdering, nySøknadsdato)
        InMemorySakRepository.oppdaterRettighetsperiode(sak.id, Periode(overstyrtDato, Tid.MAKS))

        service.backfillBehandling(
            lagSakMedRettighetsperiode(sak, overstyrtDato),
            revurdering,
            erNyesteBehandling = true,
        )

        val nyttKrav = InMemoryKravRepository.hent(revurdering.id).gjeldendeRelevanteKrav().single()
        assertThat(nyttKrav.overstyrMuligRettFra).isNotNull
        assertThat(nyttKrav.overstyrMuligRettFra!!.dato).isEqualTo(overstyrtDato)
        assertThat(nyttKrav.muligRettFra).isEqualTo(overstyrtDato) // min(5 jan 2024, 1 mar 2023)
    }

    @Test
    fun `stønadsperiodevurdering videreføres fra forrige behandling ved revurdering uten kravendring`() {
        val søknadsdato = 10 januar 2024
        val (sak, førstegangsbehandling, revurdering) = opprettInMemorySakOgRevurdering(søknadsdato = søknadsdato)

        leggTilSøknad(førstegangsbehandling, søknadsdato)

        service.backfillBehandling(sak, førstegangsbehandling, erNyesteBehandling = false)
        val vurderingFørstegangsbehandling =
            InMemoryStønadsperiodeRepository.hentHvisEksisterer(førstegangsbehandling.id)!!.vurderinger.single()

        service.backfillBehandling(sak, revurdering, erNyesteBehandling = true)
        val vurderingerRevurdering =
            InMemoryStønadsperiodeRepository.hentHvisEksisterer(revurdering.id)!!.vurderinger

        // Vurderingen skal videreføres uendret – ikke erstattes med en ny automatisk vurdering
        assertThat(vurderingerRevurdering).hasSize(1)
        val vurderingRevurdering = vurderingerRevurdering.single()
        assertThat(vurderingRevurdering.referanse).isEqualTo(vurderingFørstegangsbehandling.referanse)
        assertThat(vurderingRevurdering.opprettet).isEqualTo(vurderingFørstegangsbehandling.opprettet)
        assertThat(vurderingRevurdering.vurdertIBehandling).isEqualTo(vurderingFørstegangsbehandling.vurdertIBehandling)
    }

    @Test
    fun `stønadsperiodevurdering for nedgradert krav fjernes ikke – filtreres bort ved bruk`() {
        val senereSøknadsdato = 10 januar 2024
        val tidligereSøknadsdato = 5 januar 2024
        val (sak, førstegangsbehandling, revurdering) =
            opprettInMemorySakOgRevurdering(søknadsdato = senereSøknadsdato)

        leggTilSøknad(førstegangsbehandling, senereSøknadsdato)
        leggTilSøknad(revurdering, tidligereSøknadsdato)

        InMemorySakRepository.oppdaterRettighetsperiode(sak.id, Periode(tidligereSøknadsdato, Tid.MAKS))

        service.backfillBehandling(sak, førstegangsbehandling, erNyesteBehandling = false)
        val relevantKravFørstegangsbehandling =
            InMemoryKravRepository.hent(førstegangsbehandling.id).gjeldendeRelevanteKrav().single()

        service.backfillBehandling(
            lagSakMedRettighetsperiode(sak, tidligereSøknadsdato),
            revurdering,
            erNyesteBehandling = true,
        )

        val stønadsperiodeRevurdering =
            InMemoryStønadsperiodeRepository.hentHvisEksisterer(revurdering.id)!!.vurderinger

        // Stønadsperiodevurderingen for det gamle (nå nedgraderte) kravet skal fortsatt finnes i settet
        assertThat(stønadsperiodeRevurdering.map { it.referanse })
            .contains(relevantKravFørstegangsbehandling.referanse)

        // ...men skal filtreres bort når den vurderes mot gjeldende krav (nedgradert krav er ikke lenger relevant)
        val kravRevurdering = InMemoryKravRepository.hent(revurdering.id)
        val gjeldendeKravReferanser = kravRevurdering.gjeldendeRelevanteKrav().map { it.referanse }
        assertThat(gjeldendeKravReferanser).doesNotContain(relevantKravFørstegangsbehandling.referanse)
    }

    private fun opprettSakOgBehandlingMedSøknad(
        søknadsdato: LocalDate,
        rettighetsperiodeFom: LocalDate = søknadsdato,
    ): Pair<Sak, Behandling> {
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = søknadsdato)
        leggTilSøknad(behandling, søknadsdato)

        val sakMedRettighetsperiode = lagSakMedRettighetsperiode(sak, rettighetsperiodeFom)
        return sakMedRettighetsperiode to behandling
    }

    private fun opprettSakMedToSøknader(): Pair<Sak, Behandling> {
        val søknadsdato = 10 januar 2024
        val andresøknadsdato = 20 januar 2024
        val (sak, behandling) = opprettInMemorySakOgBehandling(søknadsdato = søknadsdato)
        leggTilSøknad(behandling, søknadsdato)
        leggTilSøknad(behandling, andresøknadsdato)
        return lagSakMedRettighetsperiode(sak, søknadsdato) to behandling
    }

    private fun leggTilSøknad(behandling: Behandling, dato: LocalDate) {
        InMemoryMottattDokumentRepository.lagre(
            MottattDokument(
                referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, UUID.randomUUID().toString()),
                sakId = behandling.sakId,
                behandlingId = behandling.id,
                mottattTidspunkt = dato.atStartOfDay(),
                type = InnsendingType.SØKNAD,
                kanal = Kanal.DIGITAL,
                strukturertDokument = null,
            )
        )
    }

    private fun leggTilLegeerklæring(behandling: Behandling, dato: LocalDate) {
        InMemoryMottattDokumentRepository.lagre(
            MottattDokument(
                referanse = InnsendingReferanse(InnsendingReferanse.Type.JOURNALPOST, UUID.randomUUID().toString()),
                sakId = behandling.sakId,
                behandlingId = behandling.id,
                mottattTidspunkt = dato.atStartOfDay(),
                type = InnsendingType.LEGEERKLÆRING,
                kanal = Kanal.DIGITAL,
                strukturertDokument = null,
            )
        )
    }

    private fun lagSakMedRettighetsperiode(sak: Sak, fom: LocalDate): Sak {
        return Sak(
            id = sak.id,
            saksnummer = sak.saksnummer,
            person = sak.person,
            rettighetsperiode = Periode(fom, fom.plusYears(1)),
            opprettetTidspunkt = sak.opprettetTidspunkt,
        )
    }

    private fun assertHarNøyaktigEttRelevantKrav(vurderinger: Set<KravVurdering>): RelevantKrav {
        val relevanteKrav = vurderinger.gjeldendeVurderinger().filterIsInstance<RelevantKrav>()
        assertThat(relevanteKrav).hasSize(1)
        return relevanteKrav.single()
    }

    private fun lagRettighetsperiodeVurdering(
        harRett: RettighetsperiodeHarRett,
        startDato: LocalDate?,
    ) = RettighetsperiodeVurdering(
        startDato = startDato,
        begrunnelse = "Testbegrunnelse",
        harRettUtoverSøknadsdato = harRett,
        vurdertAv = Bruker("Z999999"),
        vurdertDato = LocalDateTime.of(2024, 1, 1, 12, 0),
    )

    private fun lagTrukketSøknadVurdering(skalTrekkes: Boolean) = TrukketSøknadVurdering(
        journalpostId = JournalpostId("JP-${UUID.randomUUID()}"),
        begrunnelse = "Feilregistrert",
        skalTrekkes = skalTrekkes,
        vurdertAv = Bruker("Z999999"),
        vurdert = Instant.now(),
        aarsak = AarsakTilTrekkSoknad.BRUKER_SOKTE_FOR_TIDLIG,
    )
}
