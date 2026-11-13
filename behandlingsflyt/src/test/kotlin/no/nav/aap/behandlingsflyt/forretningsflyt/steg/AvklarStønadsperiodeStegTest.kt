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
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemorySakRepository
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.behandlingsflyt.test.september
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryArenaMaksdatoRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryBehandlingRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryKravRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryStønadsperiodeRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class AvklarStønadsperiodeStegTest {

    private lateinit var steg: AvklarStønadsperiodeSteg
    private val sakService = SakService(InMemorySakRepository, InMemoryBehandlingRepository)

    @BeforeEach
    fun setup() {
        steg = lagSteg(KantIKantPåskruddUnleash)
    }

    private fun lagSteg(unleash: UnleashGateway) = AvklarStønadsperiodeSteg(
        unleashGateway = unleash,
        kravRepository = InMemoryKravRepository,
        stønadsperiodeRepository = InMemoryStønadsperiodeRepository,
        arenaMaksdatoRepository = InMemoryArenaMaksdatoRepository,
        sakService = sakService,
    )

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

    // Kant i kant med Arena. Maksdato er 1. august 2024, så overgangen fra Arena er 2. august 2024.

    @Test
    fun `ett krav før overgangen - kant i kant fra dagen etter maksdato`() {
        val behandling = opprettFørstegangsbehandling(opprettInMemorySak().id)
        val krav = lagRelevantKrav(behandling.id, 1 juni 2024)

        utførMedKravOgMaksdato(behandling.id, setOf(krav), MAKSDATO)

        val vurdering = hentVurdering(behandling.id, krav)
        assertKantIKant(vurdering)
        assertThat(vurdering.relevantKravType).isEqualTo(RelevantKravType.NY_STØNADSPERIODE)
        assertThat(vurdering.harGjenværendeKvote).isFalse()
        assertThat(vurdering.begrunnelse)
            .isEqualTo("Startdatoen er dagen etter siste dag med rett til AAP i tilstøtende Arena-sak 2023-5")
    }

    @Test
    fun `ett krav som starter på maksdato - kant i kant fra dagen etter maksdato`() {
        val behandling = opprettFørstegangsbehandling(opprettInMemorySak().id)
        val krav = lagRelevantKrav(behandling.id, MAKSDATO)

        utførMedKravOgMaksdato(behandling.id, setOf(krav), MAKSDATO)

        assertKantIKant(hentVurdering(behandling.id, krav))
    }

    @Test
    fun `ett krav som starter dagen etter maksdato - kant i kant`() {
        val behandling = opprettFørstegangsbehandling(opprettInMemorySak().id)
        val krav = lagRelevantKrav(behandling.id, DAGEN_ETTER_MAKSDATO)

        utførMedKravOgMaksdato(behandling.id, setOf(krav), MAKSDATO)

        assertKantIKant(hentVurdering(behandling.id, krav))
    }

    @Test
    fun `opphold etter maksdato - vanlig vurdering fra mulig rett fra`() {
        val behandling = opprettFørstegangsbehandling(opprettInMemorySak().id)
        val krav = lagRelevantKrav(behandling.id, 3 august 2024)

        utførMedKravOgMaksdato(behandling.id, setOf(krav), MAKSDATO)

        val vurdering = hentVurdering(behandling.id, krav)
        assertVanligVurdering(vurdering, 3 august 2024)
        assertThat(vurdering.begrunnelse).isEqualTo("Automatisk vurdering")
    }

    @Test
    fun `kravet som dekker overgangen er kant i kant - senere krav får vanlig vurdering`() {
        val behandling = opprettFørstegangsbehandling(opprettInMemorySak().id)
        val kravVedOvergang = lagRelevantKrav(behandling.id, 1 juni 2024)
        val senereKrav = lagRelevantKrav(behandling.id, 1 september 2024)

        utførMedKravOgMaksdato(behandling.id, setOf(senereKrav, kravVedOvergang), MAKSDATO)

        assertKantIKant(hentVurdering(behandling.id, kravVedOvergang))
        assertVanligVurdering(hentVurdering(behandling.id, senereKrav), 1 september 2024)
    }

    @Test
    fun `alle krav starter etter overgangen - ingen sperre og vanlig vurdering for alle`() {
        val behandling = opprettFørstegangsbehandling(opprettInMemorySak().id)
        val førsteKrav = lagRelevantKrav(behandling.id, 3 august 2024)
        val andreKrav = lagRelevantKrav(behandling.id, 1 september 2024)

        utførMedKravOgMaksdato(behandling.id, setOf(førsteKrav, andreKrav), MAKSDATO)

        assertVanligVurdering(hentVurdering(behandling.id, førsteKrav), 3 august 2024)
        assertVanligVurdering(hentVurdering(behandling.id, andreKrav), 1 september 2024)
    }

    @Test
    fun `senere krav dekker overgangen - stopper uten å lagre`() {
        val sak = opprettInMemorySak()
        val forrigeBehandling = opprettFørstegangsbehandling(sak.id)
        val tidligereKrav = lagRelevantKrav(forrigeBehandling.id, 1 januar 2024)
        InMemoryKravRepository.lagre(forrigeBehandling.id, setOf(tidligereKrav))
        InMemoryStønadsperiodeRepository.lagre(
            forrigeBehandling.id,
            setOf(lagStønadsperiodeVurdering(tidligereKrav.referanse, forrigeBehandling.id, tidligereKrav.muligRettFra)),
        )

        val behandling = opprettRevurdering(sak.id, forrigeBehandling.id)
        val kravVedOvergang = lagRelevantKrav(behandling.id, 1 juni 2024)
        InMemoryKravRepository.lagre(behandling.id, setOf(kravVedOvergang, tidligereKrav))
        InMemoryArenaMaksdatoRepository.lagre(behandling.id, lagArenaMaksdato(MAKSDATO))

        assertStopperPåTidligereKrav(behandling.id, kravVedOvergang)
        assertThat(InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)).isNull()
    }

    @Test
    fun `senere krav starter dagen etter maksdato - stopper uten å lagre`() {
        val behandling = opprettFørstegangsbehandling(opprettInMemorySak().id)
        val tidligereKrav = lagRelevantKrav(behandling.id, 1 juni 2024)
        val kravVedOvergang = lagRelevantKrav(behandling.id, DAGEN_ETTER_MAKSDATO)
        InMemoryKravRepository.lagre(behandling.id, setOf(tidligereKrav, kravVedOvergang))
        InMemoryArenaMaksdatoRepository.lagre(behandling.id, lagArenaMaksdato(MAKSDATO))

        assertStopperPåTidligereKrav(behandling.id, kravVedOvergang)
        assertThat(InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)).isNull()
    }

    @Test
    fun `sperren endrer ikke allerede lagret grunnlag`() {
        val behandling = opprettFørstegangsbehandling(opprettInMemorySak().id)
        val kravVedOvergang = lagRelevantKrav(behandling.id, DAGEN_ETTER_MAKSDATO)
        utførMedKravOgMaksdato(behandling.id, setOf(kravVedOvergang), MAKSDATO)
        val lagretGrunnlag = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)

        val tidligereKrav = lagRelevantKrav(behandling.id, 1 juni 2024)
        InMemoryKravRepository.lagre(behandling.id, setOf(tidligereKrav, kravVedOvergang))

        assertStopperPåTidligereKrav(behandling.id, kravVedOvergang)
        assertThat(InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)).isEqualTo(lagretGrunnlag)
    }

    @Test
    fun `arena-sak uten maksdato - vanlig vurdering og ingen sperre`() {
        val behandling = opprettFørstegangsbehandling(opprettInMemorySak().id)
        val førsteKrav = lagRelevantKrav(behandling.id, 1 januar 2024)
        val andreKrav = lagRelevantKrav(behandling.id, 1 juni 2024)

        utførMedKravOgMaksdato(behandling.id, setOf(førsteKrav, andreKrav), maksdato = null)

        assertVanligVurdering(hentVurdering(behandling.id, førsteKrav), 1 januar 2024)
        assertVanligVurdering(hentVurdering(behandling.id, andreKrav), 1 juni 2024)
    }

    @Test
    fun `ingen arena-sak - vanlig vurdering og ingen sperre`() {
        val behandling = opprettFørstegangsbehandling(opprettInMemorySak().id)
        val førsteKrav = lagRelevantKrav(behandling.id, 1 januar 2024)
        val andreKrav = lagRelevantKrav(behandling.id, 1 juni 2024)
        InMemoryKravRepository.lagre(behandling.id, setOf(førsteKrav, andreKrav))
        InMemoryArenaMaksdatoRepository.lagre(behandling.id, null)

        steg.utfør(flytKontekstMedPerioder { this.behandling = InMemoryBehandlingRepository.hent(behandling.id) })

        assertVanligVurdering(hentVurdering(behandling.id, førsteKrav), 1 januar 2024)
        assertVanligVurdering(hentVurdering(behandling.id, andreKrav), 1 juni 2024)
    }

    @Test
    fun `toggle av - maksdato fra Arena brukes ikke og sperren slår ikke til`() {
        val stegUtenToggle = lagSteg(KravStegPåskruddUnleash)
        val behandling = opprettFørstegangsbehandling(opprettInMemorySak().id)
        val førsteKrav = lagRelevantKrav(behandling.id, 1 januar 2024)
        val andreKrav = lagRelevantKrav(behandling.id, 1 juni 2024)
        InMemoryKravRepository.lagre(behandling.id, setOf(førsteKrav, andreKrav))
        InMemoryArenaMaksdatoRepository.lagre(behandling.id, lagArenaMaksdato(MAKSDATO))

        stegUtenToggle.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        assertVanligVurdering(hentVurdering(behandling.id, førsteKrav), 1 januar 2024)
        assertVanligVurdering(hentVurdering(behandling.id, andreKrav), 1 juni 2024)
    }

    @Test
    fun `revurdering med uendret maksdato - arver kant-i-kant-vurdering fra forrige behandling`() {
        val sak = opprettInMemorySak()
        val forrigeBehandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(forrigeBehandling.id, 1 juni 2024)
        utførMedKravOgMaksdato(forrigeBehandling.id, setOf(krav), MAKSDATO)

        val behandling = opprettRevurdering(sak.id, forrigeBehandling.id)
        utførMedKravOgMaksdato(behandling.id, setOf(krav), MAKSDATO)

        val vurdering = hentVurdering(behandling.id, krav)
        assertKantIKant(vurdering)
        assertThat(vurdering.vurdertIBehandling).isEqualTo(forrigeBehandling.id)
    }

    @Test
    fun `re-kjøring med kant i kant gir samme grunnlag`() {
        val behandling = opprettFørstegangsbehandling(opprettInMemorySak().id)
        val krav = lagRelevantKrav(behandling.id, 1 juni 2024)

        utførMedKravOgMaksdato(behandling.id, setOf(krav), MAKSDATO)
        val førsteKjøring = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger
        utførMedKravOgMaksdato(behandling.id, setOf(krav), MAKSDATO)
        val andreKjøring = InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandling.id)!!.vurderinger

        assertThat(andreKjøring).hasSize(1)
        assertThat(andreKjøring.single())
            .usingRecursiveComparison()
            .ignoringFields("opprettet")
            .isEqualTo(førsteKjøring.single())
        assertKantIKant(andreKjøring.single())
    }

    @Test
    fun `kant i kant - flytter rettighetsperioden til dagen etter maksdato`() {
        val sak = opprettInMemorySak(søknadsdato = 1 juni 2024)
        val behandling = opprettFørstegangsbehandling(sak.id)

        utførMedKravOgMaksdato(behandling.id, setOf(lagRelevantKrav(behandling.id, 1 juni 2024)), MAKSDATO)

        assertThat(sakService.hent(sak.id).rettighetsperiode)
            .isEqualTo(Periode(DAGEN_ETTER_MAKSDATO, Tid.MAKS))
    }

    @Test
    fun `kant i kant - re-kjøring gir samme rettighetsperiode`() {
        val sak = opprettInMemorySak(søknadsdato = 1 juni 2024)
        val behandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(behandling.id, 1 juni 2024)

        utførMedKravOgMaksdato(behandling.id, setOf(krav), MAKSDATO)
        utførMedKravOgMaksdato(behandling.id, setOf(krav), MAKSDATO)

        assertThat(sakService.hent(sak.id).rettighetsperiode.fom).isEqualTo(DAGEN_ETTER_MAKSDATO)
    }

    @Test
    fun `revurdering - guard hindrer at rettighetsperioden flyttes for vedtatt sak`() {
        val sak = opprettInMemorySak(søknadsdato = 1 juni 2024)
        val forrigeBehandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(forrigeBehandling.id, 1 juni 2024)
        utførMedKravOgMaksdato(forrigeBehandling.id, setOf(krav), maksdato = null)

        val behandling = opprettRevurdering(sak.id, forrigeBehandling.id)
        utførMedKravOgMaksdato(behandling.id, setOf(krav), MAKSDATO)

        assertKantIKant(hentVurdering(behandling.id, krav))
        assertThat(sakService.hent(sak.id).rettighetsperiode.fom).isEqualTo(1 juni 2024)
    }

    @Test
    fun `maksdato fjernet ved re-kjøring - setter rettighetsperioden tilbake til kravdato`() {
        val sak = opprettInMemorySak(søknadsdato = 1 juni 2024)
        val behandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(behandling.id, 1 juni 2024)

        utførMedKravOgMaksdato(behandling.id, setOf(krav), MAKSDATO)
        utførMedKravOgMaksdato(behandling.id, setOf(krav), maksdato = null)

        assertThat(sakService.hent(sak.id).rettighetsperiode)
            .isEqualTo(Periode(1 juni 2024, Tid.MAKS))
    }

    @Test
    fun `maksdato flyttet senere - rettighetsperioden følger ny maksdato`() {
        val sak = opprettInMemorySak(søknadsdato = 1 juni 2024)
        val behandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(behandling.id, 1 juni 2024)

        utførMedKravOgMaksdato(behandling.id, setOf(krav), MAKSDATO)
        utførMedKravOgMaksdato(behandling.id, setOf(krav), 15 august 2024)

        assertThat(sakService.hent(sak.id).rettighetsperiode.fom).isEqualTo(16 august 2024)
    }

    @Test
    fun `maksdato flyttet tidligere - rettighetsperioden følger ny maksdato`() {
        val sak = opprettInMemorySak(søknadsdato = 1 juni 2024)
        val behandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(behandling.id, 1 juni 2024)

        utførMedKravOgMaksdato(behandling.id, setOf(krav), MAKSDATO)
        utførMedKravOgMaksdato(behandling.id, setOf(krav), 15 juli 2024)

        assertThat(sakService.hent(sak.id).rettighetsperiode.fom).isEqualTo(16 juli 2024)
    }

    @Test
    fun `toggle slått av etter første kjøring - rettighetsperioden endres ikke`() {
        val sak = opprettInMemorySak(søknadsdato = 1 juni 2024)
        val behandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(behandling.id, 1 juni 2024)
        utførMedKravOgMaksdato(behandling.id, setOf(krav), MAKSDATO)

        lagSteg(KravStegPåskruddUnleash).utfør(flytKontekstMedPerioder { this.behandling = behandling })

        assertThat(sakService.hent(sak.id).rettighetsperiode.fom).isEqualTo(DAGEN_ETTER_MAKSDATO)
    }

    @Test
    fun `avvik uten kant i kant - rettighetsperioden endres ikke`() {
        val sak = opprettInMemorySak(søknadsdato = 1 mai 2024)
        val behandling = opprettFørstegangsbehandling(sak.id)

        utførMedKravOgMaksdato(behandling.id, setOf(lagRelevantKrav(behandling.id, 1 juni 2024)), maksdato = null)

        assertThat(sakService.hent(sak.id).rettighetsperiode.fom).isEqualTo(1 mai 2024)
    }

    @Test
    fun `fom endret av andre etter kant i kant - settes ikke tilbake`() {
        val sak = opprettInMemorySak(søknadsdato = 1 juni 2024)
        val behandling = opprettFørstegangsbehandling(sak.id)
        val krav = lagRelevantKrav(behandling.id, 1 juni 2024)
        utførMedKravOgMaksdato(behandling.id, setOf(krav), MAKSDATO)
        sakService.overstyrRettighetsperioden(sakService.hent(sak.id), 1 mai 2024, Tid.MAKS)

        utførMedKravOgMaksdato(behandling.id, setOf(krav), maksdato = null)

        assertThat(sakService.hent(sak.id).rettighetsperiode.fom).isEqualTo(1 mai 2024)
    }

    @Test
    fun `ikke kant i kant - rettighetsperioden endres ikke`() {
        val sak = opprettInMemorySak(søknadsdato = 1 juni 2024)
        val behandling = opprettFørstegangsbehandling(sak.id)

        utførMedKravOgMaksdato(behandling.id, setOf(lagRelevantKrav(behandling.id, 1 juni 2024)), maksdato = null)

        assertThat(sakService.hent(sak.id).rettighetsperiode.fom).isEqualTo(1 juni 2024)
    }

    @Test
    fun `toggle av - rettighetsperioden endres ikke`() {
        val stegUtenToggle = lagSteg(KravStegPåskruddUnleash)
        val sak = opprettInMemorySak(søknadsdato = 1 juni 2024)
        val behandling = opprettFørstegangsbehandling(sak.id)
        InMemoryKravRepository.lagre(behandling.id, setOf(lagRelevantKrav(behandling.id, 1 juni 2024)))
        InMemoryArenaMaksdatoRepository.lagre(behandling.id, lagArenaMaksdato(MAKSDATO))

        stegUtenToggle.utfør(flytKontekstMedPerioder { this.behandling = behandling })

        assertThat(sakService.hent(sak.id).rettighetsperiode.fom).isEqualTo(1 juni 2024)
    }

    private fun utførMedKravOgMaksdato(behandlingId: BehandlingId, krav: Set<RelevantKrav>, maksdato: LocalDate?) {
        InMemoryKravRepository.lagre(behandlingId, krav)
        InMemoryArenaMaksdatoRepository.lagre(behandlingId, lagArenaMaksdato(maksdato))
        steg.utfør(flytKontekstMedPerioder { this.behandling = InMemoryBehandlingRepository.hent(behandlingId) })
    }

    private fun hentVurdering(behandlingId: BehandlingId, krav: RelevantKrav): StønadsperiodeVurdering =
        InMemoryStønadsperiodeRepository.hentHvisEksisterer(behandlingId)!!
            .gjeldendeVurderinger()
            .single { it.referanse == krav.referanse }

    private fun assertKantIKant(vurdering: StønadsperiodeVurdering) {
        assertThat(vurdering.startDato).isEqualTo(DAGEN_ETTER_MAKSDATO)
        assertThat(vurdering.harHattOrdinærSiste52Uker).isTrue()
        assertThat(vurdering.erKantIKantMedArena).isTrue()
    }

    private fun assertVanligVurdering(vurdering: StønadsperiodeVurdering, startDato: LocalDate) {
        assertThat(vurdering.startDato).isEqualTo(startDato)
        assertThat(vurdering.harHattOrdinærSiste52Uker).isFalse()
        assertThat(vurdering.erKantIKantMedArena).isFalse()
    }

    private fun assertStopperPåTidligereKrav(behandlingId: BehandlingId, kravVedOvergang: RelevantKrav) {
        assertThatThrownBy {
            steg.utfør(flytKontekstMedPerioder { this.behandling = InMemoryBehandlingRepository.hent(behandlingId) })
        }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining(kravVedOvergang.referanse.verdi.toString())
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

    private companion object {
        val MAKSDATO: LocalDate = 1 august 2024
        val DAGEN_ETTER_MAKSDATO: LocalDate = 2 august 2024
    }
}
