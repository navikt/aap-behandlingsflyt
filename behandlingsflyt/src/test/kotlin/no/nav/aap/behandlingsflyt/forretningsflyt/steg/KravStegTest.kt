package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
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
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryMottattDokumentRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
    fun `ingen søknader - grunnlag forblir null`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)

        every { behandlingService.utledFaktiskBehandlingstype(behandling.id) } returns TypeBehandling.Førstegangsbehandling

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        assertThat(InMemoryKravRepository.hentHvisEksisterer(behandling.id)).isNull()
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
        
        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryKravRepository.hent(behandling.id).vurderinger
        assertThat(vurderinger).hasSize(2)
        assertThat(vurderinger.filterIsInstance<RelevantKrav>()).hasSize(1)
        assertThat(vurderinger.filterIsInstance<RelevantKrav>().first().vurdertIBehandling).isEqualTo(forrigeBehandling.id)
        assertThat(vurderinger.filterIsInstance<Tilleggsopplysning>()).hasSize(1)
        assertThat(vurderinger.filterIsInstance<Tilleggsopplysning>().first().vurdertIBehandling).isEqualTo(behandling.id)
    }

    @Test
    fun `revurdering etter rent avslag bevarer NyttKrav fra forrige behandling og oppretter nytt`() {
        val sak = opprettInMemorySak()
        val forrigeBehandling = opprettFørstegangsbehandling(sak.id)
        val behandling = opprettRevurdering(sak.id, forrigeBehandling.id)

        val gammeltNyttKrav = lagNyttKrav(forrigeBehandling.id, JournalpostId("JP001"), LocalDate.of(2023, 1, 1))
        InMemoryKravRepository.lagre(forrigeBehandling.id, setOf(gammeltNyttKrav))

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
    fun `overstyrt krav i inneværende behandling bevares og søknad lagres som Tilleggsopplysning`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)

        val overstyrtKrav = lagNyttKrav(
            behandlingId = behandling.id,
            journalpostId = JournalpostId("JP_OVERSTYRT"),
            mottattDato = LocalDate.of(2024, 1, 1),
            overstyrMuligRettFra = OverstyrMuligRettFra(
                dato = LocalDate.of(2023, 6, 1),
                årsak = OverstyrMuligRettFraÅrsak.IkkeIStandTilÅSøkeTidligere,
            ),
        )
        InMemoryKravRepository.lagre(behandling.id, setOf(overstyrtKrav))

        leggTilSøknad(behandling.id, sak.id, LocalDate.of(2024, 3, 1).atStartOfDay())

        every { behandlingService.utledFaktiskBehandlingstype(behandling.id) } returns TypeBehandling.Førstegangsbehandling

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryKravRepository.hent(behandling.id).vurderinger
        assertThat(vurderinger.filterIsInstance<RelevantKrav>()).hasSize(1)
        assertThat(vurderinger.filterIsInstance<RelevantKrav>().single().referanse).isEqualTo(overstyrtKrav.referanse)
        assertThat(vurderinger.filterIsInstance<Tilleggsopplysning>()).hasSize(1)
    }

    @Test
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
            ),
        )
        InMemoryKravRepository.lagre(forrigeBehandling.id, setOf(overstyrtKravFraForrige))

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
        søknadsdato = Søknadsdato(mottattDato, SøknadsdatoÅrsak.SøknadMottatt),
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
}
