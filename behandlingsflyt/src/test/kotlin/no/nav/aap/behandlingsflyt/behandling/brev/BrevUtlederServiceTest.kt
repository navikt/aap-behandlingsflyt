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
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
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
        unleashGateway = unleashGateway,
    )
    val forventetSisteDagMedYtelse = 31 august 2025

    @Test
    fun `utledBehov legger ved sisteDagMedYtelse fra underveisTidslinje i TilkjentYtelse`() {
        val virkningstidspunkt = LocalDate.of(2025, 1, 1)
        val førstegangsbehandling = behandling(typeBehandling = TypeBehandling.Førstegangsbehandling)
        every { vedtakRepository.hent(any<BehandlingId>()) } returns Vedtak(
            behandlingId = førstegangsbehandling.id,
            vedtakstidspunkt = LocalDateTime.of(2025,1,1, 0, 0,0),
            virkningstidspunkt = virkningstidspunkt,
        )
        every { beregningsgrunnlagRepository.hentHvisEksisterer(førstegangsbehandling.id) } returns Grunnlag11_19(
            grunnlaget = GUnit(2),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(0),
            inntekter = listOf(
                grunnlagInntekt(2024, 220_000),
                grunnlagInntekt(2023, 210_000),
                grunnlagInntekt(2022, 200_000),
            )
        )
        every { beregningVurderingRepository.hentHvisEksisterer(førstegangsbehandling.id) } returns BeregningGrunnlag(
            BeregningstidspunktVurdering(
                begrunnelse = "",
                nedsattArbeidsevneEllerStudieevneDato = LocalDate.of(2025, 1, 1),
                ytterligereNedsattBegrunnelse = null,
                ytterligereNedsattArbeidsevneDato = null,
                vurdertAv = ""
            ), null
        )
        every { behandlingRepository.hent(any<BehandlingId>()) } returns førstegangsbehandling
        every { trukketSøknadService.søknadErTrukket(any<BehandlingId>()) } returns false
        every { underveisRepository.hent(førstegangsbehandling.id) } returns underveisGrunnlag()
        every { arbeidsopptrappingRepository.hentPerioder(any<BehandlingId>()) } returns emptyList()

        val dagsats = Beløp("1000.00")
        every { tilkjentYtelseRepository.hentHvisEksisterer(førstegangsbehandling.id)} returns tilkjentYtelseForFørstegangsbehandling(dagsats)
        every { underveisRepository.hentHvisEksisterer(førstegangsbehandling.id) } returns underveisGrunnlag()
        every { sykdomsvurderingForBrevRepository.hent(førstegangsbehandling.id)} returns sykdomsvurderingForBrevGrunnlag()

        val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(førstegangsbehandling.id)

        assertIs<Innvilgelse>(resultat, "forventer brevbehov er av typen Innvilgelse")

        assertNotNull(resultat.tilkjentYtelse, "tilkjent ytelse må eksistere")
        assertEquals(forventetSisteDagMedYtelse, resultat.tilkjentYtelse.sisteDagMedYtelse)
    }

    @Test
    fun `utledBehov legger ved 3 alternative beløp for årlig ytelse`() {
        val virkningstidspunkt = LocalDate.of(2025, 1, 1)
        val førstegangsbehandling = behandling(typeBehandling = TypeBehandling.Førstegangsbehandling)
        every { vedtakRepository.hent(any<BehandlingId>()) } returns Vedtak(
            behandlingId = førstegangsbehandling.id,
            vedtakstidspunkt = LocalDateTime.of(2025,1,1, 0, 0,0),
            virkningstidspunkt = virkningstidspunkt,
        )
        every { beregningsgrunnlagRepository.hentHvisEksisterer(førstegangsbehandling.id) } returns Grunnlag11_19(
            grunnlaget = GUnit(2),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(0),
            inntekter = listOf(
                grunnlagInntekt(2024, 220_000),
                grunnlagInntekt(2023, 210_000),
                grunnlagInntekt(2022, 200_000),
            )
        )
        every { beregningVurderingRepository.hentHvisEksisterer(førstegangsbehandling.id) } returns BeregningGrunnlag(
            BeregningstidspunktVurdering(
                begrunnelse = "",
                nedsattArbeidsevneEllerStudieevneDato = LocalDate.of(2025, 1, 1),
                ytterligereNedsattBegrunnelse = null,
                ytterligereNedsattArbeidsevneDato = null,
                vurdertAv = ""
            ), null
        )
        every { behandlingRepository.hent(any<BehandlingId>()) } returns førstegangsbehandling
        every { trukketSøknadService.søknadErTrukket(any<BehandlingId>()) } returns false
        every { underveisRepository.hent(førstegangsbehandling.id) } returns underveisGrunnlag()
        every { arbeidsopptrappingRepository.hentPerioder(any<BehandlingId>()) } returns emptyList()

        val dagsats = Beløp("1000.00")
        every { tilkjentYtelseRepository.hentHvisEksisterer(førstegangsbehandling.id)} returns tilkjentYtelseForFørstegangsbehandling(dagsats)
        every { underveisRepository.hentHvisEksisterer(førstegangsbehandling.id) } returns underveisGrunnlag()
        every { sykdomsvurderingForBrevRepository.hent(førstegangsbehandling.id)} returns sykdomsvurderingForBrevGrunnlag()

        val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(førstegangsbehandling.id)

        assertIs<Innvilgelse>(resultat, "forventer brevbehov er av typen Innvilgelse")

        assertNotNull(resultat.tilkjentYtelse, "tilkjent ytelse må eksistere")
        // Gitt tilkjent dagsats på 1000.00 NOK, så skal årligYtelse være dagsats x 260 dager for fullt år
        val forventetÅrligYtelse = dagsats.multiplisert(260)
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
        val virkningstidspunkt = LocalDate.of(2025, 1, 1)
        val førstegangsbehandling = behandling(typeBehandling = TypeBehandling.Førstegangsbehandling)
        every { vedtakRepository.hent(any<BehandlingId>()) } returns Vedtak(
            behandlingId = førstegangsbehandling.id,
            vedtakstidspunkt = LocalDateTime.of(2025,1,1, 0, 0,0),
            virkningstidspunkt = virkningstidspunkt,
        )

        every { beregningsgrunnlagRepository.hentHvisEksisterer(førstegangsbehandling.id) } returns Grunnlag11_19(
            grunnlaget = GUnit(2),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(0),
            inntekter = listOf(
                grunnlagInntekt(2024, 220_000),
                grunnlagInntekt(2023, 210_000),
                grunnlagInntekt(2022, 200_000),
            )
        )

        every { beregningVurderingRepository.hentHvisEksisterer(førstegangsbehandling.id) } returns BeregningGrunnlag(
            BeregningstidspunktVurdering(
                begrunnelse = "",
                nedsattArbeidsevneEllerStudieevneDato = LocalDate.of(2025, 1, 1),
                ytterligereNedsattBegrunnelse = null,
                ytterligereNedsattArbeidsevneDato = null,
                vurdertAv = ""
            ), null
        )

        every { behandlingRepository.hent(any<BehandlingId>()) } returns førstegangsbehandling
        every { trukketSøknadService.søknadErTrukket(any<BehandlingId>()) } returns false
        every { underveisRepository.hent(førstegangsbehandling.id) } returns underveisGrunnlag()
        every { arbeidsopptrappingRepository.hentPerioder(any<BehandlingId>()) } returns emptyList()

        val dagsats = Beløp("1000.00")
        every { tilkjentYtelseRepository.hentHvisEksisterer(førstegangsbehandling.id)} returns tilkjentYtelseForFørstegangsbehandling(dagsats)
        every { underveisRepository.hentHvisEksisterer(førstegangsbehandling.id) } returns underveisGrunnlag()
        every { sykdomsvurderingForBrevRepository.hent(førstegangsbehandling.id)} returns sykdomsvurderingForBrevGrunnlag()

        val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(førstegangsbehandling.id)
        assertIs<Innvilgelse>(resultat, "brevbehov er av type Innvilgelse")
        assertEquals(resultat.sykdomsvurdering, "Vurdering av sykdom")
    }

    @Test
    fun `skal hente ut sykdomsvurdering ved avslag`() {
        val førstegangsbehandling = behandling(typeBehandling = TypeBehandling.Førstegangsbehandling)
        every { vedtakRepository.hent(any<BehandlingId>()) } returns Vedtak(
            behandlingId = førstegangsbehandling.id,
            vedtakstidspunkt = LocalDateTime.of(2025,1,1, 0, 0,0),
            virkningstidspunkt = null
        )

        every { beregningsgrunnlagRepository.hentHvisEksisterer(førstegangsbehandling.id) } returns Grunnlag11_19(
            grunnlaget = GUnit(2),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(0),
            inntekter = listOf(
                grunnlagInntekt(2024, 220_000),
                grunnlagInntekt(2023, 210_000),
                grunnlagInntekt(2022, 200_000),
            )
        )

        every { beregningVurderingRepository.hentHvisEksisterer(førstegangsbehandling.id) } returns BeregningGrunnlag(
            BeregningstidspunktVurdering(
                begrunnelse = "",
                nedsattArbeidsevneEllerStudieevneDato = LocalDate.of(2025, 1, 1),
                ytterligereNedsattBegrunnelse = null,
                ytterligereNedsattArbeidsevneDato = null,
                vurdertAv = ""
            ), null
        )

        every { behandlingRepository.hent(any<BehandlingId>()) } returns førstegangsbehandling
        every { trukketSøknadService.søknadErTrukket(any<BehandlingId>()) } returns false
        every { underveisRepository.hentHvisEksisterer(any<BehandlingId>()) } returns underveisgrunnlagAvslag()
        every { arbeidsopptrappingRepository.hentPerioder(any<BehandlingId>()) } returns emptyList()

        every { sykdomsvurderingForBrevRepository.hent(any<BehandlingId>())} returns sykdomsvurderingForBrevGrunnlag()

        val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(førstegangsbehandling.id)
        assertIs<Avslag>(resultat, "brevbehov er av type Avslag")
        assertEquals(resultat.sykdomsvurdering, "Vurdering av sykdom")
    }


    @Test
    fun `skal feile ved utleding av brevtype dersom aktivitetsplikt mangler`() {
        every { behandlingRepository.hent(any<BehandlingId>()) } returns aktivitetspliktBehandling
        every { aktivitetspliktRepository.hentHvisEksisterer(aktivitetspliktBehandling.id) } returns null
        every { arbeidsopptrappingRepository.hentPerioder(aktivitetspliktBehandling.id) } returns emptyList()
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
        every { arbeidsopptrappingRepository.hentPerioder(aktivitetspliktBehandling.id) } returns emptyList()

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
        every { arbeidsopptrappingRepository.hentPerioder(aktivitetspliktBehandling.id) } returns emptyList()

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
        every { arbeidsopptrappingRepository.hentPerioder(aktivitetspliktBehandling.id) } returns emptyList()

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
        every { arbeidsopptrappingRepository.hentPerioder(aktivitetspliktBehandling.id) } returns emptyList()

        assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(aktivitetspliktBehandling.id)).isEqualTo(
            VedtakAktivitetsplikt11_7
        )
    }

    @Test
    fun `skal utlede brev etter rettighetstype § 11-17 ved innvilgelse av revurdering`() {
        val førstegangsbehandling = behandling(typeBehandling = TypeBehandling.Førstegangsbehandling)
        val revurdering = behandling(
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
        every { avbrytRevurderingService.revurderingErAvbrutt(revurdering.id) } returns false
        every { arbeidsopptrappingRepository.hentPerioder(revurdering.id) } returns emptyList()
        every { arbeidsopptrappingRepository.hentPerioder(revurdering.forrigeBehandlingId!!) } returns emptyList()
        every { underveisRepository.hentHvisEksisterer(revurdering.id) } returns underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2023, 31 desember 2023),
                rettighetsType = RettighetsType.ARBEIDSSØKER,
                utfall = Utfall.OPPFYLT,
            )
        )

        assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id)).isEqualTo(Arbeidssøker)
    }

    @Test
    fun `skal ikke utlede brev etter rettighetstype § 11-17 ved innvilgelse av revurdering dersom det samme gjelder forrige behandling`() {
        val forrigeBehandling = behandling(typeBehandling = TypeBehandling.Revurdering)
        val revurdering = behandling(
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
        every { arbeidsopptrappingRepository.hentPerioder(revurdering.id) } returns emptyList()
        every { arbeidsopptrappingRepository.hentPerioder(revurdering.forrigeBehandlingId!!) } returns emptyList()
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
        val revurdering = behandling(
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = BehandlingId(Random.nextLong()),
            vurderingsbehov = listOf(Vurderingsbehov.OVERGANG_ARBEID)
        )
        every { unleashGateway.isEnabled(BehandlingsflytFeature.NyBrevtype11_17) } returns true
        every { behandlingRepository.hent(revurdering.id) } returns revurdering
        every { avbrytRevurderingService.revurderingErAvbrutt(revurdering.id) } returns false
        every { arbeidsopptrappingRepository.hentPerioder(revurdering.id) } returns listOf(Periode(1 januar 2024, 31 desember 2024))
        every { arbeidsopptrappingRepository.hentPerioder(revurdering.forrigeBehandlingId!!) } returns emptyList()
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
        val revurdering = behandling(
            typeBehandling = TypeBehandling.Revurdering,
            forrigeBehandlingId = BehandlingId(Random.nextLong()),
            vurderingsbehov = listOf(Vurderingsbehov.OVERGANG_ARBEID)
        )
        every { unleashGateway.isEnabled(BehandlingsflytFeature.NyBrevtype11_17) } returns true
        every { behandlingRepository.hent(revurdering.id) } returns revurdering
        every { avbrytRevurderingService.revurderingErAvbrutt(revurdering.id) } returns false
        every { arbeidsopptrappingRepository.hentPerioder(revurdering.id) } returns listOf(Periode(1 januar 2024, 31 desember 2024))
        every { arbeidsopptrappingRepository.hentPerioder(revurdering.forrigeBehandlingId!!) } returns listOf(Periode(1 januar 2023, 31 desember 2023))
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
        val førstegangsbehandling = behandling(typeBehandling = TypeBehandling.Førstegangsbehandling)
        val revurdering = behandling(
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
        every { arbeidsopptrappingRepository.hentPerioder(revurdering.id) } returns emptyList()
        every { arbeidsopptrappingRepository.hentPerioder(revurdering.forrigeBehandlingId!!) } returns emptyList()
        every { avbrytRevurderingService.revurderingErAvbrutt(revurdering.id) } returns false
        every { underveisRepository.hentHvisEksisterer(revurdering.id) } returns underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2023, 31 desember 2023),
                rettighetsType = RettighetsType.VURDERES_FOR_UFØRETRYGD,
                utfall = Utfall.OPPFYLT,
            )
        )
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
        every { beregningVurderingRepository.hentHvisEksisterer(revurdering.id) } returns BeregningGrunnlag(
            BeregningstidspunktVurdering(
                begrunnelse = "",
                nedsattArbeidsevneEllerStudieevneDato = LocalDate.of(2023, 2, 20),
                ytterligereNedsattBegrunnelse = null,
                ytterligereNedsattArbeidsevneDato = null,
                vurdertAv = ""
            ), null
        )

        assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id)).isEqualTo(
            VurderesForUføretrygd(
                GrunnlagBeregning(
                    beregningstidspunkt = LocalDate.of(2023, 2, 20),
                    inntekterPerÅr = listOf(
                        InntektPerÅr(Year.of(2024), inntekt = BigDecimal("220000.00")),
                        InntektPerÅr(Year.of(2023), inntekt = BigDecimal("210000.00")),
                        InntektPerÅr(Year.of(2022), inntekt = BigDecimal("200000.00")),
                    ),
                    beregningsgrunnlag = null
                )

            )
        )
    }

    @Test
    fun `skal ikke utlede brev etter rettighetstype § 11-18 ved innvilgelse av revurdering dersom det samme gjelder forrige behandling`() {
        val førstegangsbehandling = behandling(typeBehandling = TypeBehandling.Førstegangsbehandling)
        val revurdering = behandling(
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
        every { arbeidsopptrappingRepository.hentPerioder(revurdering.id) } returns emptyList()
        every { arbeidsopptrappingRepository.hentPerioder(revurdering.forrigeBehandlingId!!) } returns emptyList()
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

    val aktivitetspliktBehandling = behandling(
        typeBehandling = TypeBehandling.Aktivitetsplikt,
        status = Status.OPPRETTET,
        årsakTilOpprettelse = ÅrsakTilOpprettelse.AKTIVITETSPLIKT,
    )

    private fun behandling(
        typeBehandling: TypeBehandling,
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

    private fun underveisGrunnlag(): UnderveisGrunnlag {
        return underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2025, 30 april 2025),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                utfall = Utfall.OPPFYLT,
            ),
            underveisperiode(
                periode = Periode(1 mai 2025, forventetSisteDagMedYtelse),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                utfall = Utfall.OPPFYLT,
            ),
            underveisperiode(
                periode = Periode(1 september 2025, 31 desember 2025),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
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

    private fun tilkjentYtelseForFørstegangsbehandling(dagsats: Beløp): List<TilkjentYtelsePeriode> {
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
