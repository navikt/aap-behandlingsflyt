package no.nav.aap.behandlingsflyt.behandling.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.GraderingGrunnlag
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.MINSTE_ÅRLIG_YTELSE_TIDSLINJE
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.vedtak.Vedtak
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.behandlingsflyt.test.september
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.ZoneId
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class BrevUtlederServiceTest {
    val tilkjentYtelseRepository = mockk<TilkjentYtelseRepository>()
    val vedtakRepository = mockk<VedtakRepository>()
    val trukketSøknadService = mockk<TrukketSøknadService>()
    val behandlingRepository = mockk<BehandlingRepository>()
    val beregningsgrunnlagRepository = mockk<BeregningsgrunnlagRepository>()
    val beregningVurderingRepository = mockk<BeregningVurderingRepository>()
    val aktivitetspliktRepository = mockk<Aktivitetsplikt11_7Repository>()
    val arbeidsopptrappingRepository = mockk<ArbeidsopptrappingRepository>()
    val sykdomsvurderingForBrevRepository = mockk<SykdomsvurderingForBrevRepository>()
    val underveisRepository = mockk<UnderveisRepository>()
    val overgangUføreRepository = mockk<OvergangUføreRepository>()
    val avbrytRevurderingService = mockk<AvbrytRevurderingService>()
    val unleashGateway = mockk<UnleashGateway>()
    val brevUtlederService = BrevUtlederService(
        resultatUtleder = ResultatUtleder(
            underveisRepository = underveisRepository,
            behandlingRepository = behandlingRepository,
            trukketSøknadService = trukketSøknadService,
            avbrytRevurderingService = avbrytRevurderingService
        ),
        klageresultatUtleder = mockk<KlageresultatUtleder>(),
        behandlingRepository = behandlingRepository,
        vedtakRepository = vedtakRepository,
        beregningsgrunnlagRepository = beregningsgrunnlagRepository,
        beregningVurderingRepository = beregningVurderingRepository,
        tilkjentYtelseRepository = tilkjentYtelseRepository,
        underveisRepository = underveisRepository,
        aktivitetsplikt11_7Repository = aktivitetspliktRepository,
        arbeidsopptrappingRepository = arbeidsopptrappingRepository,
        sykdomsvurderingForBrevRepository = sykdomsvurderingForBrevRepository,
        overgangUføreRepository = overgangUføreRepository,
        unleashGateway = unleashGateway,
    )
    val virkningstidspunkt = 1 januar 2025

    @BeforeEach
    fun setup() {
        // felles random mocks/stubs, override i test ved behov
        every { arbeidsopptrappingRepository.hentHvisEksisterer(any<BehandlingId>()) } returns null
        every { beregningsgrunnlagRepository.hentHvisEksisterer(any<BehandlingId>()) } returns stubGrunnlag11_19()
        every { beregningVurderingRepository.hentHvisEksisterer(any<BehandlingId>()) } returns stubBeregningGrunnlag()
        every { overgangUføreRepository.hentHvisEksisterer(any<BehandlingId>()) } returns null
        every { sykdomsvurderingForBrevRepository.hent(any<BehandlingId>())} returns sykdomsvurderingForBrevGrunnlag()
        every { tilkjentYtelseRepository.hentHvisEksisterer(any<BehandlingId>()) } returns stubTilkjentYtelse()
        every { trukketSøknadService.søknadErTrukket(any<BehandlingId>()) } returns false
        every { unleashGateway.isEnabled(BehandlingsflytFeature.NyBrevtype11_17) } returns true
    }

    @Test
    fun `utledBehov legger ved sisteDagMedYtelse i TilkjentYtelse faktagrunnlag for 11-17 brev ved revurdering`() {
        val førstegangsbehandling = stubBehandling(
            typeBehandling = TypeBehandling.Førstegangsbehandling,
            status = Status.AVSLUTTET,
            årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
            vurderingsbehov = emptyList()
        )
        every { vedtakRepository.hent(førstegangsbehandling.id) } returns stubVedtak(førstegangsbehandling.id)
        every { behandlingRepository.hent(førstegangsbehandling.id) } returns førstegangsbehandling
        every { underveisRepository.hent(førstegangsbehandling.id) } returns stubUnderveisGrunnlag(rettighetsType = RettighetsType.BISTANDSBEHOV)
        every { underveisRepository.hentHvisEksisterer(førstegangsbehandling.id) } returns stubUnderveisGrunnlag(rettighetsType = RettighetsType.BISTANDSBEHOV)
        val revurdering = stubBehandling(
            typeBehandling = TypeBehandling.Revurdering,
            status = Status.OPPRETTET,
            årsakTilOpprettelse = ÅrsakTilOpprettelse.HELSEOPPLYSNINGER,
            vurderingsbehov = listOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
            forrigeBehandlingId = førstegangsbehandling.id
        )
        every { vedtakRepository.hent(revurdering.id) } returns stubVedtak(revurdering.id)
        every { behandlingRepository.hent(revurdering.id) } returns revurdering
        val gittSisteDagMedYtelse = 31 august 2025
        val underveisGrunnlag = stubUnderveisGrunnlag(gittSisteDagMedYtelse, RettighetsType.ARBEIDSSØKER)
        every { underveisRepository.hent(revurdering.id) } returns underveisGrunnlag
        every { underveisRepository.hentHvisEksisterer(revurdering.id) } returns underveisGrunnlag

        every { avbrytRevurderingService.revurderingErAvbrutt(any<BehandlingId>()) } returns false
        every { unleashGateway.isEnabled(BehandlingsflytFeature.NyBrevtype11_17) } returns true

        val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id)

        assertIs<Arbeidssøker>(resultat, "forventer brevbehov er av typen Arbeidssøker (11-17)")
        assertThat(resultat).hasFieldOrProperty("tilkjentYtelse")
        assertEquals(gittSisteDagMedYtelse, resultat.tilkjentYtelse!!.sisteDagMedYtelse)
    }

    @Test
    fun `utledBehov legger ved sisteDagMedYtelse fra underveisTidslinje i TilkjentYtelse for 11-18 brev`() {
        val behandling = stubBehandling(TypeBehandling.Førstegangsbehandling)
        every { behandlingRepository.hent(behandling.id) } returns behandling
        every { vedtakRepository.hent(behandling.id) } returns stubVedtak(behandling.id)
        val gittSisteDagMedYtelse = 31 august 2025
        val underveisGrunnlag = stubUnderveisGrunnlag(gittSisteDagMedYtelse, RettighetsType.VURDERES_FOR_UFØRETRYGD)
        every { underveisRepository.hent(behandling.id) } returns underveisGrunnlag
        every { underveisRepository.hentHvisEksisterer(behandling.id) } returns underveisGrunnlag

        val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(behandling.id)

        assertIs<VurderesForUføretrygd>(resultat, "forventer brevbehov er av typen 11-18")
        assertNotNull(resultat.tilkjentYtelse, "tilkjent ytelse må eksistere")
        assertEquals(gittSisteDagMedYtelse, resultat.tilkjentYtelse.sisteDagMedYtelse)
    }

    @Test
    fun `utledBehov legger ved sisteDagMedYtelse fra underveisTidslinje i TilkjentYtelse for Innvilgelse brev`() {
        val behandling = stubBehandling(TypeBehandling.Førstegangsbehandling)
        every { behandlingRepository.hent(behandling.id) } returns behandling
        every { vedtakRepository.hent(behandling.id) } returns stubVedtak(behandling.id)
        val gittSisteDagMedYtelse = 31 august 2025
        val underveisGrunnlag = stubUnderveisGrunnlag(gittSisteDagMedYtelse, RettighetsType.BISTANDSBEHOV)
        every { underveisRepository.hent(behandling.id) } returns underveisGrunnlag
        every { underveisRepository.hentHvisEksisterer(behandling.id) } returns underveisGrunnlag

        val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(behandling.id)

        assertIs<Innvilgelse>(resultat, "forventer brevbehov er av typen Innvilgelse")
        assertNotNull(resultat.tilkjentYtelse, "tilkjent ytelse må eksistere")
        assertEquals(gittSisteDagMedYtelse, resultat.tilkjentYtelse.sisteDagMedYtelse)
    }

    @Test
    fun `utledBehov legger ved kravdatoUføretrygd som faktagrunnlag i Tilkjentytelse for 11-18 brev`() {
        val behandling = stubBehandling(TypeBehandling.Førstegangsbehandling)
        every { behandlingRepository.hent(behandling.id) } returns behandling
        every { vedtakRepository.hent(behandling.id) } returns stubVedtak(behandling.id)
        val gittKravDatoUføretrygd = 30 november 2025
        every { overgangUføreRepository.hentHvisEksisterer(behandling.id) } returns OvergangUføreGrunnlag(
            vurderinger = listOf(
                OvergangUføreVurdering(
                    begrunnelse = "test",
                    brukerHarSøktOmUføretrygd = true,
                    brukerHarFåttVedtakOmUføretrygd = "bla..bla",
                    brukerRettPåAAP = true,
                    fom = gittKravDatoUføretrygd,
                    tom = 31 desember 2025,
                    vurdertAv = "meg",
                    vurdertIBehandling = behandling.id,
                    opprettet = LocalDateTime.now().atZone(ZoneId.of("Europe/Oslo")).toInstant(),
                )
            )
        )
        val underveisGrunnlag = stubUnderveisGrunnlag(rettighetsType = RettighetsType.VURDERES_FOR_UFØRETRYGD)
        every { underveisRepository.hent(behandling.id) } returns underveisGrunnlag
        every { underveisRepository.hentHvisEksisterer(behandling.id) } returns underveisGrunnlag

        val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(behandling.id)

        assertIs<VurderesForUføretrygd>(resultat, "forventer brevbehov er av typen 11-18")
        assertNotNull(resultat.tilkjentYtelse, "tilkjent ytelse må eksistere")
        assertEquals(gittKravDatoUføretrygd, resultat.tilkjentYtelse.kravdatoUføretrygd)
    }

    @Test
    fun `utledBehov legger ved 3 alternative beløp for årlig ytelse`() {
        val behandling = stubBehandling(TypeBehandling.Førstegangsbehandling)
        every { behandlingRepository.hent(behandling.id) } returns behandling
        every { vedtakRepository.hent(behandling.id) } returns stubVedtak(behandling.id)
        val underveisGrunnlag = stubUnderveisGrunnlag()
        every { underveisRepository.hent(behandling.id) } returns underveisGrunnlag
        every { underveisRepository.hentHvisEksisterer(behandling.id) } returns underveisGrunnlag
        val gittDagsats = Beløp("1000.00")
        every { tilkjentYtelseRepository.hentHvisEksisterer(behandling.id) } returns stubTilkjentYtelse(gittDagsats)

        val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(behandling.id)

        assertIs<Innvilgelse>(resultat, "forventer brevbehov er av typen Innvilgelse")

        assertNotNull(resultat.tilkjentYtelse, "tilkjent ytelse må eksistere")
        // Gitt tilkjent dagsats på 1000.00 NOK, så skal årligYtelse være dagsats x 260 dager for fullt år
        val forventetÅrligYtelse = gittDagsats.multiplisert(260)
        assertEquals(forventetÅrligYtelse, resultat.tilkjentYtelse.årligYtelse)

        val minsteÅrligeYtelseGUnit: GUnit = MINSTE_ÅRLIG_YTELSE_TIDSLINJE.segment(virkningstidspunkt)?.verdi!!
        // MinsteÅrligYtelse som faktagrunnlag til brevforhåndsvisning er et statisk beløp og sammenfaller kun med
        // beregnet dagsats når dagsats også er beregnet fra minstesats. testen hardkoder dagsats til 1000.00 NOK
        val grunnbeløp = Grunnbeløp.tilTidslinje().segment(virkningstidspunkt)?.verdi
        val forventetMinsteÅrligYtelse = grunnbeløp?.multiplisert(minsteÅrligeYtelseGUnit)
        assertEquals(forventetMinsteÅrligYtelse, resultat.tilkjentYtelse.minsteÅrligYtelse)

        val forventetMinsteÅrligYtelseUnder25 = grunnbeløp?.multiplisert(minsteÅrligeYtelseGUnit.toTredjedeler())
        assertEquals(forventetMinsteÅrligYtelseUnder25, resultat.tilkjentYtelse.minsteÅrligYtelseUnder25)
    }

    @Test
    fun `skal hente ut sykdomsvurdering ved innvilgelse`() {
        val behandling = stubBehandling(TypeBehandling.Førstegangsbehandling)
        every { behandlingRepository.hent(behandling.id) } returns behandling
        every { vedtakRepository.hent(behandling.id) } returns stubVedtak(behandling.id)
        val underveisGrunnlag = stubUnderveisGrunnlag()
        every { underveisRepository.hent(behandling.id) } returns underveisGrunnlag
        every { underveisRepository.hentHvisEksisterer(behandling.id) } returns underveisGrunnlag
        every { sykdomsvurderingForBrevRepository.hent(behandling.id)} returns sykdomsvurderingForBrevGrunnlag()

        val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(behandling.id)

        assertIs<Innvilgelse>(resultat, "brevbehov er av type Innvilgelse")
        assertEquals(resultat.sykdomsvurdering, "Vurdering av sykdom")
    }

    @Test
    fun `skal hente ut sykdomsvurdering ved avslag`() {
        val behandling = stubBehandling(TypeBehandling.Førstegangsbehandling)
        every { behandlingRepository.hent(behandling.id) } returns behandling
        every { vedtakRepository.hent(behandling.id) } returns stubVedtak(behandling.id)
        val underveisGrunnlag = underveisgrunnlagAvslag()
        every { underveisRepository.hent(behandling.id) } returns underveisGrunnlag
        every { underveisRepository.hentHvisEksisterer(behandling.id) } returns underveisGrunnlag
        every { sykdomsvurderingForBrevRepository.hent(behandling.id)} returns sykdomsvurderingForBrevGrunnlag()

        val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(behandling.id)

        assertIs<Avslag>(resultat, "brevbehov er av type Avslag")
        assertEquals(resultat.sykdomsvurdering, "Vurdering av sykdom")
    }

    @Test
    fun `faktagrunnlag for sykdomsvurdering settes til null hvis det ikke finnes`() {
        val behandling = stubBehandling(TypeBehandling.Førstegangsbehandling)
        every { behandlingRepository.hent(behandling.id) } returns behandling
        every { vedtakRepository.hent(behandling.id) } returns stubVedtak(behandling.id)
        val underveisGrunnlagAvslag = underveisgrunnlagAvslag()
        every { underveisRepository.hent(behandling.id) } returns underveisGrunnlagAvslag
        every { underveisRepository.hentHvisEksisterer(behandling.id) } returns underveisGrunnlagAvslag
        every { sykdomsvurderingForBrevRepository.hent(behandling.id) } returns null

        val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(behandling.id)

        assertIs<Avslag>(resultat, "brevbehov er av type Avslag")
        assertEquals(null, resultat.sykdomsvurdering)
    }

    @Test
    fun `skal feile ved utleding av brevtype dersom aktivitetsplikt mangler`() {
        every { behandlingRepository.hent(any<BehandlingId>()) } returns aktivitetspliktBehandling
        every { aktivitetspliktRepository.hentHvisEksisterer(aktivitetspliktBehandling.id) } returns null
        every { arbeidsopptrappingRepository.hentHvisEksisterer(aktivitetspliktBehandling.id) } returns null
        assertThrows<IllegalStateException> {
            brevUtlederService.utledBehovForMeldingOmVedtak(aktivitetspliktBehandling.id)
        }
    }

    @Test
    fun `skal utlede brevtype VedtakAktivitetsplikt11_7 når det iverksettes brudd på 11_7`() {
        every { behandlingRepository.hent(any<BehandlingId>()) } returns aktivitetspliktBehandling
        every { aktivitetspliktRepository.hentHvisEksisterer(aktivitetspliktBehandling.id) } returns Aktivitetsplikt11_7Grunnlag(
            vurderinger = listOf(aktivitetspliktBrudd(aktivitetspliktBehandling.id))
        )
        every { arbeidsopptrappingRepository.hentHvisEksisterer(aktivitetspliktBehandling.id) } returns null

        assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(aktivitetspliktBehandling.id)).isEqualTo(
            VedtakAktivitetsplikt11_7
        )
    }

    @Test
    fun `skal utlede brevtype VedtakEndring når et aktivitetsplikt brudd omgjøres`() {
        every { behandlingRepository.hent(any<BehandlingId>()) } returns aktivitetspliktBehandling
        every { aktivitetspliktRepository.hentHvisEksisterer(aktivitetspliktBehandling.id) } returns Aktivitetsplikt11_7Grunnlag(
            vurderinger = listOf(aktivitetspliktBruddOppfylt(aktivitetspliktBehandling.id))
        )
        every { arbeidsopptrappingRepository.hentHvisEksisterer(aktivitetspliktBehandling.id) } returns null

        assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(aktivitetspliktBehandling.id)).isEqualTo(
            VedtakEndring
        )
    }

    @Test
    fun `skal utlede brevtype VedtakEndring når nyeste aktivitetsplikt brudd omgjøres`() {
        every { behandlingRepository.hent(any<BehandlingId>()) } returns aktivitetspliktBehandling

        val gammelBruddDato = LocalDate.now().minusDays(100)
        every { aktivitetspliktRepository.hentHvisEksisterer(aktivitetspliktBehandling.id) } returns Aktivitetsplikt11_7Grunnlag(
            vurderinger = listOf(
                aktivitetspliktBrudd(BehandlingId(100), fra = gammelBruddDato),
                aktivitetspliktBrudd(BehandlingId(101), fra = gammelBruddDato.plusDays(1)),
                aktivitetspliktBrudd(BehandlingId(102)),
                aktivitetspliktBruddOppfylt(aktivitetspliktBehandling.id)
            )
        )
        every { arbeidsopptrappingRepository.hentHvisEksisterer(aktivitetspliktBehandling.id) } returns null

        assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(aktivitetspliktBehandling.id)).isEqualTo(
            VedtakEndring
        )
    }

    @Test
    fun `skal utlede brevtype Aktivitetspliktbrudd når nyeste aktivitetsplikt brudd er stans`() {
        every { behandlingRepository.hent(any<BehandlingId>()) } returns aktivitetspliktBehandling

        val gammelBruddDato = LocalDate.now().minusDays(100)
        every { aktivitetspliktRepository.hentHvisEksisterer(aktivitetspliktBehandling.id) } returns Aktivitetsplikt11_7Grunnlag(
            vurderinger = listOf(
                aktivitetspliktBrudd(BehandlingId(100), fra = gammelBruddDato),
                aktivitetspliktBrudd(BehandlingId(101), fra = gammelBruddDato.plusDays(1)),
                aktivitetspliktBruddOppfylt(BehandlingId(102)),
                aktivitetspliktBrudd(aktivitetspliktBehandling.id)
            )
        )
        every { arbeidsopptrappingRepository.hentHvisEksisterer(aktivitetspliktBehandling.id) } returns null

        assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(aktivitetspliktBehandling.id)).isEqualTo(
            VedtakAktivitetsplikt11_7
        )
    }

    @Test
    fun `skal utlede brev etter rettighetstype § 11-17 ved innvilgelse av revurdering`() {
        val førstegangsbehandling = stubBehandling(TypeBehandling.Førstegangsbehandling)
        every { behandlingRepository.hent(førstegangsbehandling.id) } returns førstegangsbehandling
        every { vedtakRepository.hent(førstegangsbehandling.id) } returns stubVedtak(førstegangsbehandling.id)
        val underveisGrunnlagAvslag = underveisgrunnlagAvslag()
        every { underveisRepository.hent(førstegangsbehandling.id) } returns underveisGrunnlagAvslag
        every { underveisRepository.hentHvisEksisterer(førstegangsbehandling.id) } returns underveisGrunnlagAvslag
        every { sykdomsvurderingForBrevRepository.hent(førstegangsbehandling.id) } returns null
        val revurdering = stubBehandling(
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = førstegangsbehandling.id,
            vurderingsbehov = listOf(Vurderingsbehov.OVERGANG_ARBEID)
        )
        every { unleashGateway.isEnabled(BehandlingsflytFeature.NyBrevtype11_17) } returns true
        every { underveisRepository.hentHvisEksisterer(førstegangsbehandling.id) } returns underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2023, 31 desember 2023),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                utfall = Utfall.OPPFYLT,
            )
        )
        every { behandlingRepository.hent(revurdering.id) } returns revurdering
        every { vedtakRepository.hent(revurdering.id) } returns stubVedtak(revurdering.id)
        every { avbrytRevurderingService.revurderingErAvbrutt(revurdering.id) } returns false
        every { arbeidsopptrappingRepository.hentHvisEksisterer(revurdering.id) } returns null
        every { arbeidsopptrappingRepository.hentHvisEksisterer(revurdering.forrigeBehandlingId!!) } returns null
        val underveisGrunnlag = underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2023, 31 desember 2023),
                rettighetsType = RettighetsType.ARBEIDSSØKER,
                utfall = Utfall.OPPFYLT,
            )
        )
        every { underveisRepository.hent(revurdering.id) } returns underveisGrunnlag
        every { underveisRepository.hentHvisEksisterer(revurdering.id) } returns underveisGrunnlag

        val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id)

        assertIs<Arbeidssøker>(resultat, "brevbehov er av type Arbeidssøker")
    }

    @Test
    fun `skal ikke utlede brev etter rettighetstype § 11-17 ved innvilgelse av revurdering dersom det samme gjelder forrige behandling`() {
        val forrigeBehandling = stubBehandling(typeBehandling = TypeBehandling.Revurdering)
        val revurdering = stubBehandling(
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = forrigeBehandling.id,
            vurderingsbehov = listOf(Vurderingsbehov.OVERGANG_ARBEID)
        )
        every { unleashGateway.isEnabled(BehandlingsflytFeature.NyBrevtype11_17) } returns true
        every { underveisRepository.hentHvisEksisterer(forrigeBehandling.id) } returns underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2023, 31 desember 2023),
                rettighetsType = RettighetsType.ARBEIDSSØKER,
                utfall = Utfall.OPPFYLT,
            )
        )
        every { behandlingRepository.hent(revurdering.id) } returns revurdering
        every { avbrytRevurderingService.revurderingErAvbrutt(revurdering.id) } returns false
        every { arbeidsopptrappingRepository.hentHvisEksisterer(revurdering.id) } returns null
        every { arbeidsopptrappingRepository.hentHvisEksisterer(revurdering.forrigeBehandlingId!!) } returns null
        every { underveisRepository.hentHvisEksisterer(revurdering.id) } returns underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2023, 31 desember 2023),
                rettighetsType = RettighetsType.ARBEIDSSØKER,
                utfall = Utfall.OPPFYLT,
            )
        )

        assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id)).isEqualTo(VedtakEndring)
    }

    @Test
    fun `skal utlede brev for § 11-23 sjette ledd ved arbeidsopptrapping på gjeldende behandling og ikke på forrige behandling`() {
        val revurdering = stubBehandling(
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = BehandlingId(Random.nextLong()),
            vurderingsbehov = listOf(Vurderingsbehov.OVERGANG_ARBEID)
        )
        every { unleashGateway.isEnabled(BehandlingsflytFeature.NyBrevtype11_17) } returns true
        every { behandlingRepository.hent(revurdering.id) } returns revurdering
        every { avbrytRevurderingService.revurderingErAvbrutt(revurdering.id) } returns false
        every { arbeidsopptrappingRepository.hentHvisEksisterer(revurdering.id) } returns arbeidsopptrappingGrunnlag(
            revurdering,
            1 januar 2024,
            31 desember 2024
        )
        every { arbeidsopptrappingRepository.hentHvisEksisterer(revurdering.forrigeBehandlingId!!) } returns null
        every { underveisRepository.hentHvisEksisterer(revurdering.id) } returns underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2024, 31 desember 2024),
                rettighetsType = RettighetsType.ARBEIDSSØKER,
                utfall = Utfall.OPPFYLT,
            )
        )

        assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id))
            .isEqualTo(VedtakArbeidsopptrapping11_23_sjette_ledd)
    }

    @Test
    fun `skal ikke utlede brev for § 11-23 sjette ledd ved arbeidsopptrapping på gjeldende behandling i tillegg til forrige behandling`() {
        val revurdering = stubBehandling(
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = BehandlingId(Random.nextLong()),
            vurderingsbehov = listOf(Vurderingsbehov.OVERGANG_ARBEID)
        )
        every { unleashGateway.isEnabled(BehandlingsflytFeature.NyBrevtype11_17) } returns true
        every { behandlingRepository.hent(revurdering.id) } returns revurdering
        every { avbrytRevurderingService.revurderingErAvbrutt(revurdering.id) } returns false
        every { arbeidsopptrappingRepository.hentHvisEksisterer(revurdering.id) } returns arbeidsopptrappingGrunnlag(
            revurdering,
            1 januar 2024,
            31 desember 2024
        )
        every { arbeidsopptrappingRepository.hentHvisEksisterer(revurdering.forrigeBehandlingId!!) } returns arbeidsopptrappingGrunnlag(
            revurdering,
            1 januar 2023,
            31 desember 2023
        )
        every { underveisRepository.hentHvisEksisterer(revurdering.id) } returns underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2024, 31 desember 2024),
                rettighetsType = RettighetsType.ARBEIDSSØKER,
                utfall = Utfall.OPPFYLT,
            )
        )
        every { underveisRepository.hentHvisEksisterer(revurdering.forrigeBehandlingId!!) } returns underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2023, 31 desember 2023),
                rettighetsType = RettighetsType.ARBEIDSSØKER,
                utfall = Utfall.OPPFYLT,
            )
        )

        assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id))
            .isNotEqualTo(VedtakArbeidsopptrapping11_23_sjette_ledd)
    }

    @Test
    fun `skal utlede brev etter rettighetstype § 11-18 ved innvilgelse av revurdering`() {
        val førstegangsbehandling = stubBehandling(TypeBehandling.Førstegangsbehandling)
        val revurdering = stubBehandling(
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = førstegangsbehandling.id,
            vurderingsbehov = listOf(Vurderingsbehov.OVERGANG_UFORE)
        )
        every { underveisRepository.hentHvisEksisterer(førstegangsbehandling.id) } returns underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2023, 31 desember 2023),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                utfall = Utfall.OPPFYLT,
            )
        )
        every { behandlingRepository.hent(revurdering.id) } returns revurdering
        every { arbeidsopptrappingRepository.hentHvisEksisterer(revurdering.id) } returns null
        every { arbeidsopptrappingRepository.hentHvisEksisterer(revurdering.forrigeBehandlingId!!) } returns null
        every { avbrytRevurderingService.revurderingErAvbrutt(revurdering.id) } returns false
        val sisteDagMedYtelse = 31 desember 2023
        val underveisGrunnlag = underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2023, sisteDagMedYtelse),
                rettighetsType = RettighetsType.VURDERES_FOR_UFØRETRYGD,
                utfall = Utfall.OPPFYLT,
            )
        )
        every { underveisRepository.hentHvisEksisterer(revurdering.id) } returns underveisGrunnlag
        every { underveisRepository.hent(revurdering.id) } returns underveisGrunnlag
        every { beregningsgrunnlagRepository.hentHvisEksisterer(revurdering.id) } returns Grunnlag11_19(
            grunnlaget = GUnit(2),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(0),
            inntekter = listOf(
                grunnlagInntekt(2024, 220_000),
                grunnlagInntekt(2023, 210_000),
                grunnlagInntekt(2022, 200_000),
            )
        )
        val virkningstidspunkt = LocalDate.of(2023, 2, 20)
        every { beregningVurderingRepository.hentHvisEksisterer(revurdering.id) } returns BeregningGrunnlag(
            BeregningstidspunktVurdering(
                begrunnelse = "",
                nedsattArbeidsevneEllerStudieevneDato = virkningstidspunkt,
                ytterligereNedsattBegrunnelse = null,
                ytterligereNedsattArbeidsevneDato = null,
                vurdertAv = ""
            ), null
        )
        every { vedtakRepository.hent(revurdering.id) } returns Vedtak(revurdering.id, LocalDateTime.now(), virkningstidspunkt)
        every { tilkjentYtelseRepository.hentHvisEksisterer(revurdering.id) } returns null
        every { unleashGateway.isEnabled(any()) } returns false

        assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id)).isEqualTo(
            VurderesForUføretrygd(
                sisteDagMedYtelse = sisteDagMedYtelse,
                grunnlagBeregning = GrunnlagBeregning(
                    beregningstidspunkt = LocalDate.of(2023, 2, 20),
                    inntekterPerÅr = listOf(
                        InntektPerÅr(Year.of(2024), inntekt = BigDecimal("220000.00")),
                        InntektPerÅr(Year.of(2023), inntekt = BigDecimal("210000.00")),
                        InntektPerÅr(Year.of(2022), inntekt = BigDecimal("200000.00")),
                    ),
                    beregningsgrunnlag = null
                ),
                tilkjentYtelse = null


            )
        )
    }

    @Test
    fun `skal ikke utlede brev etter rettighetstype § 11-18 ved innvilgelse av revurdering dersom det samme gjelder forrige behandling`() {
        val førstegangsbehandling = stubBehandling(TypeBehandling.Førstegangsbehandling)
        val revurdering = stubBehandling(
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = førstegangsbehandling.id,
            vurderingsbehov = listOf(Vurderingsbehov.OVERGANG_UFORE)
        )
        every { unleashGateway.isEnabled(BehandlingsflytFeature.NyBrevtype11_17) } returns true
        every { underveisRepository.hentHvisEksisterer(førstegangsbehandling.id) } returns underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2023, 31 desember 2023),
                rettighetsType = RettighetsType.VURDERES_FOR_UFØRETRYGD,
                utfall = Utfall.OPPFYLT,
            )
        )
        every { behandlingRepository.hent(revurdering.id) } returns revurdering
        every { avbrytRevurderingService.revurderingErAvbrutt(revurdering.id) } returns false
        every { arbeidsopptrappingRepository.hentHvisEksisterer(revurdering.id) } returns null
        every { arbeidsopptrappingRepository.hentHvisEksisterer(revurdering.forrigeBehandlingId!!) } returns null
        every { underveisRepository.hentHvisEksisterer(revurdering.id) } returns underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2023, 31 desember 2023),
                rettighetsType = RettighetsType.VURDERES_FOR_UFØRETRYGD,
                utfall = Utfall.OPPFYLT,
            )
        )

        assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id)).isEqualTo(
            VedtakEndring
        )
    }

    private fun arbeidsopptrappingGrunnlag(
        revurdering: Behandling,
        fraDato: LocalDate,
        tilDato: LocalDate
    ): ArbeidsopptrappingGrunnlag = ArbeidsopptrappingGrunnlag(
        listOf(
            ArbeidsopptrappingVurdering(
                begrunnelse = "...",
                vurderingenGjelderFra = fraDato,
                reellMulighetTilOpptrapping = true,
                rettPaaAAPIOpptrapping = true,
                vurdertAv = "Bruker",
                opprettetTid = Instant.now(),
                vurdertIBehandling = revurdering.id,
                vurderingenGjelderTil = tilDato
            )
        )
    )


    private fun grunnlagInntekt(år: Int, inntekt: Int): GrunnlagInntekt {
        return GrunnlagInntekt(
            år = Year.of(år),
            inntektIKroner = Beløp(inntekt),
            grunnbeløp = Beløp(0),
            inntektIG = GUnit(0),
            inntekt6GBegrenset = GUnit(0),
            er6GBegrenset = false
        )
    }

    private fun aktivitetspliktBruddOppfylt(
        id: BehandlingId,
        fra: LocalDate = LocalDate.now()
    ): Aktivitetsplikt11_7Vurdering {
        return aktivitetspliktBrudd(id, fra).copy(erOppfylt = true, utfall = null)
    }

    private fun aktivitetspliktBrudd(id: BehandlingId, fra: LocalDate = LocalDate.now()): Aktivitetsplikt11_7Vurdering {
        return Aktivitetsplikt11_7Vurdering(
            begrunnelse = "",
            erOppfylt = false,
            utfall = no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall.STANS,
            vurdertAv = "",
            fom = fra,
            opprettet = Instant.now(),
            vurdertIBehandling = id,
            skalIgnorereVarselFrist = false
        )
    }

    val aktivitetspliktBehandling = stubBehandling(
        typeBehandling = TypeBehandling.Aktivitetsplikt,
        status = Status.OPPRETTET,
        årsakTilOpprettelse = ÅrsakTilOpprettelse.AKTIVITETSPLIKT,
    )

    private fun stubBehandling(
        typeBehandling: TypeBehandling = TypeBehandling.Førstegangsbehandling,
        id: BehandlingId = BehandlingId(Random.nextLong()),
        forrigeBehandlingId: BehandlingId? = null,
        sakId: SakId = SakId(Random.nextLong()),
        status: Status = Status.OPPRETTET,
        årsakTilOpprettelse: ÅrsakTilOpprettelse? = null,
        vurderingsbehov: List<Vurderingsbehov> = emptyList()
    ): Behandling {
        return Behandling(
            id = id,
            forrigeBehandlingId = forrigeBehandlingId,
            sakId = sakId,
            typeBehandling = typeBehandling,
            status = status,
            årsakTilOpprettelse = årsakTilOpprettelse,
            vurderingsbehov = vurderingsbehov.map { VurderingsbehovMedPeriode(it) },
            versjon = 1
        )
    }

    private fun underveisGrunnlag(vararg underveisperioder: Underveisperiode): UnderveisGrunnlag {
        return UnderveisGrunnlag(
            Random.nextLong(),
            underveisperioder.toList()
        )
    }

    private fun underveisperiode(
        periode: Periode,
        rettighetsType: RettighetsType,
        utfall: Utfall,
        avslagsårsak: UnderveisÅrsak? = null,
    ): Underveisperiode {
        return Underveisperiode(
            periode = periode,
            meldePeriode = periode,
            utfall = utfall,
            rettighetsType = rettighetsType,
            avslagsårsak = avslagsårsak,
            grenseverdi = Prosent.`100_PROSENT`,
            institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
            arbeidsgradering = ArbeidsGradering(
                totaltAntallTimer = TimerArbeid(BigDecimal(0)),
                andelArbeid = Prosent.`0_PROSENT`,
                fastsattArbeidsevne = Prosent.`100_PROSENT`,
                gradering = Prosent.`100_PROSENT`,
                opplysningerMottatt = null,
            ),
            trekk = Dagsatser(0),
            brukerAvKvoter = emptySet(),
            meldepliktStatus = null,
            meldepliktGradering = Prosent.`0_PROSENT`,
        )
    }

    private fun stubUnderveisGrunnlag(
        sisteDagMedYtelse : LocalDate = 31 august 2025,
        rettighetsType: RettighetsType = RettighetsType.BISTANDSBEHOV,
    ): UnderveisGrunnlag {
        return underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2025, 30 april 2025),
                rettighetsType = rettighetsType,
                utfall = Utfall.OPPFYLT,
            ),
            underveisperiode(
                periode = Periode(1 mai 2025, sisteDagMedYtelse),
                rettighetsType = rettighetsType,
                utfall = Utfall.OPPFYLT,
            ),
            underveisperiode(
                periode = Periode(1 september 2025, 31 desember 2025),
                rettighetsType = rettighetsType,
                utfall = Utfall.IKKE_VURDERT,
            )
        )
    }

    private fun sykdomsvurderingForBrevGrunnlag(): SykdomsvurderingForBrev {
        return SykdomsvurderingForBrev(
            behandlingId = BehandlingId(Random.nextLong()),
            vurdering = "Vurdering av sykdom",
            vurdertAv = "Veileder",
            vurdertTidspunkt = LocalDateTime.now(),
        )
    }

    private fun underveisgrunnlagAvslag(): UnderveisGrunnlag {
        return underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2025, 31 desember 2025),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                utfall = Utfall.IKKE_OPPFYLT,
                avslagsårsak = UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT
            ),
        )
    }

    private fun stubTilkjentYtelse(
        dagsats: Beløp = Beløp("1000.00")
    ): List<TilkjentYtelsePeriode> {
        val førstePeriode = Periode(1 januar 2025, 30 juni 2025)
        val andrePeriode = Periode(1 juli 2025, 30 desember 2025)
        return listOf(
            TilkjentYtelsePeriode(
                periode = førstePeriode,
                tilkjent = tilkjentYtelseDto(dagsats, førstePeriode.tom)
            ),
            TilkjentYtelsePeriode(
                periode = andrePeriode,
                tilkjent = tilkjentYtelseDto(dagsats, andrePeriode.tom)
            )
        )
    }

    private fun stubBeregningGrunnlag(): BeregningGrunnlag {
        return BeregningGrunnlag(
            tidspunktVurdering = BeregningstidspunktVurdering(
                begrunnelse = "",
                nedsattArbeidsevneEllerStudieevneDato = virkningstidspunkt,
                ytterligereNedsattBegrunnelse = null,
                ytterligereNedsattArbeidsevneDato = null,
                vurdertAv = ""
            ),
            yrkesskadeBeløpVurdering = null
        )
    }

    private fun stubGrunnlag11_19(): Beregningsgrunnlag {
        return Grunnlag11_19(
            grunnlaget = GUnit(2),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(0),
            inntekter = listOf(
                grunnlagInntekt(2024, 220_000),
                grunnlagInntekt(2023, 210_000),
                grunnlagInntekt(2022, 200_000),
            )
        )
    }

    private fun stubVedtak(behandlingId : BehandlingId): Vedtak {
        return Vedtak(
            behandlingId = behandlingId,
            vedtakstidspunkt = virkningstidspunkt.atStartOfDay(),
            virkningstidspunkt = virkningstidspunkt,
        )
    }

    private fun tilkjentYtelseDto(dagsats: Beløp, utbetalingsdato: LocalDate): Tilkjent {
        return Tilkjent(
            dagsats = dagsats,
            gradering = Prosent.`100_PROSENT`,
            graderingGrunnlag = GraderingGrunnlag(
                samordningGradering = Prosent.`0_PROSENT`,
                institusjonGradering = Prosent.`0_PROSENT`,
                arbeidGradering = Prosent.`0_PROSENT`,
                samordningUføregradering = Prosent.`0_PROSENT`,
                samordningArbeidsgiverGradering = Prosent.`0_PROSENT`,
                meldepliktGradering = Prosent.`0_PROSENT`
            ),
            grunnlagsfaktor = GUnit(1),
            grunnbeløp = Beløp(300000),
            antallBarn = 0,
            barnetilleggsats = Beløp(0),
            barnetillegg = Beløp(0),
            utbetalingsdato = utbetalingsdato
        )
    }
}
