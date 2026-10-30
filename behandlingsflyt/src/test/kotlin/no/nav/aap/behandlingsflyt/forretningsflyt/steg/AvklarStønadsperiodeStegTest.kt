package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MigrertRettighetstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeVurdering
import no.nav.aap.behandlingsflyt.help.flytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.help.opprettInMemorySak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryArenaMaksdatoRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryStønadsperiodeRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class AvklarStønadsperiodeStegTest {

    private lateinit var steg: AvklarStønadsperiodeSteg

    @BeforeEach
    fun setup() {
        steg = AvklarStønadsperiodeSteg(
            unleashGateway = KantIKantPåskruddUnleash,
            kravRepository = InMemoryKravRepository,
            stønadsperiodeRepository = InMemoryStønadsperiodeRepository,
            arenaMaksdatoRepository = InMemoryArenaMaksdatoRepository,
        )
    }

    @Test
    fun `ingen relevante krav - lagrer tomt grunnlag`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val grunnlag = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)
        assertThat(grunnlag).isNotNull()
        assertThat(grunnlag!!.vurderinger).isEmpty()
    }

    @Test
    fun `førstegangsbehandling med ett relevant krav - lagrer én vurdering med riktig referanse`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(behandling.id, LocalDate.of(2024, 1, 15))
        InMemoryKravRepository.lagre(behandling.id, setOf(krav))

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger
        assertThat(vurderinger).hasSize(1)
        assertThat(vurderinger.single().referanse).isEqualTo(krav.referanse)
        assertThat(vurderinger.single().relevantKravType).isEqualTo(RelevantKravType.NY_STØNADSPERIODE)
    }

    @Test
    fun `revurdering - arver vedtatte stønadsperioder fra forrige behandling og lager kun nye`() {
        val sak = opprettInMemorySak()
        val forrigeBehandling = opprettFørstegangsbehandling(sak.id)
        val behandling = opprettRevurdering(sak.id, forrigeBehandling.id)

        val kravFraForrige = lagRelevantKrav(forrigeBehandling.id, LocalDate.of(2023, 6, 1))
        val stønadsperiodeFraForrige = lagStønadsperiodeVurdering(kravFraForrige.referanse, forrigeBehandling.id, kravFraForrige.muligRettFra)
        InMemoryStønadsperiodeRepository.lagre(forrigeBehandling.id, setOf(stønadsperiodeFraForrige))

        val nyttKrav = lagRelevantKrav(behandling.id, LocalDate.of(2024, 6, 1))
        InMemoryKravRepository.lagre(behandling.id, setOf(kravFraForrige, nyttKrav))

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger
        assertThat(vurderinger).hasSize(2)
        assertThat(vurderinger.map { it.referanse })
            .containsExactlyInAnyOrder(kravFraForrige.referanse, nyttKrav.referanse)
        assertThat(vurderinger.single { it.referanse == kravFraForrige.referanse }.vurdertIBehandling)
            .isEqualTo(forrigeBehandling.id)
        assertThat(vurderinger.single { it.referanse == nyttKrav.referanse }.vurdertIBehandling)
            .isEqualTo(behandling.id)
    }

    @Test
    fun `re-kjøring av steget gir stabilt grunnlag uten duplikater`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(behandling.id, LocalDate.of(2024, 1, 1))
        InMemoryKravRepository.lagre(behandling.id, setOf(krav))

        val kontekst = flytKontekstMedPerioder { this.behandling = behandling }
        steg.utfør(kontekst)
        steg.utfør(kontekst)

        val vurderinger = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger
        assertThat(vurderinger).hasSize(1)
        assertThat(vurderinger.single().referanse).isEqualTo(krav.referanse)
    }

    @Test
    fun `migrering fra Arena - lagrer migrert stønadsperiode med riktig startdato og begrunnelse`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val migrertKrav = lagMigrertKrav(behandling.id, LocalDate.of(2020, 3, 1))
        InMemoryKravRepository.lagre(behandling.id, setOf(migrertKrav))

        steg.utfør(flytKontekstMedPerioder {
            this.behandling = behandling
            this.vurderingType = VurderingType.MIGERING_FRA_ARENA
        })

        val vurderinger = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger
        assertThat(vurderinger).hasSize(1)
        val vurdering = vurderinger.single()
        assertThat(vurdering.referanse).isEqualTo(migrertKrav.referanse)
        assertThat(vurdering.relevantKravType).isEqualTo(RelevantKravType.MIGRERT_STØNADSPERIODE)
        assertThat(vurdering.startDato).isEqualTo(migrertKrav.muligRettFra)
        assertThat(vurdering.harHattOrdinærSiste52Uker).isFalse()
        assertThat(vurdering.harGjenværendeKvote).isFalse()
        assertThat(vurdering.begrunnelse).contains(migrertKrav.arenaSaksnummer)
    }

    @Test
    fun `migrering fra Arena uten migrert krav - lagrer tomt grunnlag`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)

        steg.utfør(flytKontekstMedPerioder {
            this.behandling = behandling
            this.vurderingType = VurderingType.MIGERING_FRA_ARENA
        })

        val vurderinger = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)?.vurderinger
        assertThat(vurderinger).isNullOrEmpty()
    }

    @Test
    fun `første krav før maksdato - stønadsperiode starter dagen etter maksdato`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(behandling.id, 15 januar 2024)
        InMemoryKravRepository.lagre(behandling.id, setOf(krav))
        InMemoryArenaMaksdatoRepository.lagre(behandling.id, lagArenaMaksdato(31 januar 2024))

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurdering = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger.single()
        assertThat(vurdering.startDato).isEqualTo(1 februar 2024)
        assertThat(vurdering.relevantKravType).isEqualTo(RelevantKravType.NY_STØNADSPERIODE)
        assertThat(vurdering.harGjenværendeKvote).isFalse()
        assertThat(vurdering.harHattOrdinærSiste52Uker).isTrue()
        assertThat(vurdering.begrunnelse)
            .isEqualTo("Tilstøtende avsluttet Arena-sak 2023-5, stønadsperiode starter dagen etter maksdato 31.01.2024")
    }

    @Test
    fun `første krav dagen etter maksdato - stønadsperiode starter dagen etter maksdato`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(behandling.id, 1 februar 2024)
        InMemoryKravRepository.lagre(behandling.id, setOf(krav))
        InMemoryArenaMaksdatoRepository.lagre(behandling.id, lagArenaMaksdato(31 januar 2024))

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurdering = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger.single()
        assertThat(vurdering.startDato).isEqualTo(1 februar 2024)
        assertThat(vurdering.harHattOrdinærSiste52Uker).isTrue()
    }

    @Test
    fun `gap mellom maksdato og krav - stønadsperiode starter fra mulig rett fra`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(behandling.id, 2 februar 2024)
        InMemoryKravRepository.lagre(behandling.id, setOf(krav))
        InMemoryArenaMaksdatoRepository.lagre(behandling.id, lagArenaMaksdato(31 januar 2024))

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurdering = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger.single()
        assertThat(vurdering.startDato).isEqualTo(2 februar 2024)
        assertThat(vurdering.harHattOrdinærSiste52Uker).isFalse()
        assertThat(vurdering.begrunnelse).isEqualTo("Automatisk vurdering")
    }

    @Test
    fun `arena-sak uten maksdato - stønadsperiode starter fra mulig rett fra`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(behandling.id, 15 januar 2024)
        InMemoryKravRepository.lagre(behandling.id, setOf(krav))
        InMemoryArenaMaksdatoRepository.lagre(behandling.id, lagArenaMaksdato(null))

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurdering = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger.single()
        assertThat(vurdering.startDato).isEqualTo(15 januar 2024)
    }

    @Test
    fun `ingen arena-sak - stønadsperiode starter fra mulig rett fra`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(behandling.id, 15 januar 2024)
        InMemoryKravRepository.lagre(behandling.id, setOf(krav))
        InMemoryArenaMaksdatoRepository.lagre(behandling.id, null)

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurdering = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger.single()
        assertThat(vurdering.startDato).isEqualTo(15 januar 2024)
    }

    @Test
    fun `toggle av - maksdato fra Arena brukes ikke`() {
        val stegUtenToggle = AvklarStønadsperiodeSteg(
            unleashGateway = KravStegPåskruddUnleash,
            kravRepository = InMemoryKravRepository,
            stønadsperiodeRepository = InMemoryStønadsperiodeRepository,
            arenaMaksdatoRepository = InMemoryArenaMaksdatoRepository,
        )
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(behandling.id, 15 januar 2024)
        InMemoryKravRepository.lagre(behandling.id, setOf(krav))
        InMemoryArenaMaksdatoRepository.lagre(behandling.id, lagArenaMaksdato(31 januar 2024))

        stegUtenToggle.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurdering = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger.single()
        assertThat(vurdering.startDato).isEqualTo(15 januar 2024)
    }

    @Test
    fun `flere krav - kun første krav får start dagen etter maksdato`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val førsteKrav = lagRelevantKrav(behandling.id, 15 januar 2024)
        val andreKrav = lagRelevantKrav(behandling.id, 1 mars 2024)
        InMemoryKravRepository.lagre(behandling.id, setOf(førsteKrav, andreKrav))
        InMemoryArenaMaksdatoRepository.lagre(behandling.id, lagArenaMaksdato(31 januar 2024))

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger
        assertThat(vurderinger.single { it.referanse == førsteKrav.referanse }.startDato).isEqualTo(1 februar 2024)
        assertThat(vurderinger.single { it.referanse == andreKrav.referanse }.startDato).isEqualTo(1 mars 2024)
    }

    @Test
    fun `dagen etter maksdato er ikke innenfor første krav - stønadsperiode starter fra mulig rett fra`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val førsteKrav = lagRelevantKrav(behandling.id, 15 januar 2024)
        val andreKrav = lagRelevantKrav(behandling.id, 1 februar 2024)
        InMemoryKravRepository.lagre(behandling.id, setOf(førsteKrav, andreKrav))
        InMemoryArenaMaksdatoRepository.lagre(behandling.id, lagArenaMaksdato(31 januar 2024))

        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurderinger = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger
        assertThat(vurderinger.single { it.referanse == førsteKrav.referanse }.startDato).isEqualTo(15 januar 2024)
        assertThat(vurderinger.single { it.referanse == andreKrav.referanse }.startDato).isEqualTo(1 februar 2024)
    }

    @Test
    fun `revurdering med uendret maksdato - arver kant-i-kant-vurdering fra forrige behandling`() {
        val sak = opprettInMemorySak()
        val forrigeBehandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(forrigeBehandling.id, 15 januar 2024)
        InMemoryKravRepository.lagre(forrigeBehandling.id, setOf(krav))
        InMemoryArenaMaksdatoRepository.lagre(forrigeBehandling.id, lagArenaMaksdato(31 januar 2024))
        steg.utfør(flytKontekstMedPerioder { this.behandling = forrigeBehandling })

        val behandling = opprettRevurdering(sak.id, forrigeBehandling.id)
        InMemoryKravRepository.lagre(behandling.id, setOf(krav))
        InMemoryArenaMaksdatoRepository.lagre(behandling.id, lagArenaMaksdato(31 januar 2024))
        steg.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        val vurdering = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger.single()
        assertThat(vurdering.startDato).isEqualTo(1 februar 2024)
        assertThat(vurdering.vurdertIBehandling).isEqualTo(forrigeBehandling.id)
    }

    @Test
    fun `re-kjøring med kant-i-kant gir stabilt grunnlag`() {
        val sak = opprettInMemorySak()
        val behandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(behandling.id, 15 januar 2024)
        InMemoryKravRepository.lagre(behandling.id, setOf(krav))
        InMemoryArenaMaksdatoRepository.lagre(behandling.id, lagArenaMaksdato(31 januar 2024))

        val kontekst = flytKontekstMedPerioder { this.behandling = behandling }
        steg.utfør(kontekst)
        steg.utfør(kontekst)

        val vurdering = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger.single()
        assertThat(vurdering.startDato).isEqualTo(1 februar 2024)
    }

    private fun lagArenaMaksdato(maksdato: LocalDate?) = ArenaMaksdato(
        arenaSaksnummer = "2023-5",
        status = "AVSLU",
        maksdato = maksdato,
        vedtakTil = maksdato,
        vedtakId = 1,
    )

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

    private fun lagRelevantKrav(
        behandlingId: BehandlingId,
        mottattDato: LocalDate,
    ) = RelevantKrav(
        referanse = Kravreferanse.ny(),
        journalpostId = JournalpostId(mottattDato.toString()),
        vurdertAv = SYSTEMBRUKER,
        begrunnelse = "Test",
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
        søknadsdato = Søknadsdato(mottattDato, SøknadsdatoÅrsak.SøknadMottatt, begrunnelse = "Test"),
        overstyrMuligRettFra = null,
        muligRettFra = mottattDato,
    )

    private fun lagMigrertKrav(
        behandlingId: BehandlingId,
        muligRettFra: LocalDate,
    ) = MigrertKrav(
        referanse = Kravreferanse.ny(),
        vurdertAv = SYSTEMBRUKER,
        begrunnelse = "Migrert fra Arena",
        vurdertIBehandling = behandlingId,
        opprettet = Instant.now(),
        virkningstidspunktArena = muligRettFra,
        muligRettFra = muligRettFra,
        arenaSaksnummer = "ARENA-4711",
        rettighetstype = MigrertRettighetstype.ORDINÆR,
        resterendeKvoteOrdinær = 0,
    )

    private fun lagStønadsperiodeVurdering(
        referanse: Kravreferanse,
        behandlingId: BehandlingId,
        startDato: LocalDate,
    ) = StønadsperiodeVurdering(
        referanse = referanse,
        opprettet = Instant.now(),
        vurdertIBehandling = behandlingId,
        vurdertAv = SYSTEMBRUKER,
        begrunnelse = "Test",
        harHattOrdinærSiste52Uker = false,
        harGjenværendeKvote = false,
        relevantKravType = RelevantKravType.NY_STØNADSPERIODE,
        startDato = startDato,
    )

    private object KravStegPåskruddUnleash : FakeUnleashBaseWithDefaultDisabled(
        listOf(BehandlingsflytFeature.KravSteg)
    )

    private object KantIKantPåskruddUnleash : FakeUnleashBaseWithDefaultDisabled(
        listOf(BehandlingsflytFeature.KravSteg, BehandlingsflytFeature.HandtereMaksdatoForKantTilKant)
    )
}
