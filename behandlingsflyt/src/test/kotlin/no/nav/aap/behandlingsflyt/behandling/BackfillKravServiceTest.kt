package no.nav.aap.behandlingsflyt.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.rettighetsperiode.VurderRettighetsperiodeRepository
import no.nav.aap.behandlingsflyt.behandling.søknad.AarsakTilTrekkSoknad
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravVurdering
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

    // -------------------------------------------------------------------------
    // Kravutledning
    // -------------------------------------------------------------------------

    @Test
    fun `første søknad i sak gir RelevantKrav med riktig dato`() {
        val søknadsdato = 10 januar 2024
        val (sak, behandling) = opprettSakOgBehandlingMedSøknad(søknadsdato)

        service.backfillBehandling(sak, behandling, erNyesteBehandling = true)

        val krav = InMemoryKravRepository.hent(behandling.id)
        val relevantKrav = assertHarNøyaktigEnRelevantKrav(krav.vurderinger)
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
        assertHarNøyaktigEnRelevantKrav(kravRevurdering.vurderinger)
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

        val krav = assertHarNøyaktigEnRelevantKrav(InMemoryKravRepository.hent(behandling.id).vurderinger)
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

        val krav = assertHarNøyaktigEnRelevantKrav(InMemoryKravRepository.hent(behandling.id).vurderinger)
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

    // -------------------------------------------------------------------------
    // Stønadsperiode
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Hjelpefunksjoner
    // -------------------------------------------------------------------------

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

    private fun lagSakMedRettighetsperiode(sak: Sak, fom: LocalDate): Sak {
        return Sak(
            id = sak.id,
            saksnummer = sak.saksnummer,
            person = sak.person,
            rettighetsperiode = Periode(fom, fom.plusYears(1)),
            opprettetTidspunkt = sak.opprettetTidspunkt,
        )
    }

    private fun assertHarNøyaktigEnRelevantKrav(vurderinger: Set<KravVurdering>): RelevantKrav {
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
