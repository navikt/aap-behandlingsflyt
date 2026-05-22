package no.nav.aap.behandlingsflyt.behandling.brev

import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.GraderingGrunnlag
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.MINSTE_ÅRLIG_YTELSE_TIDSLINJE
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Minstesats
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseGrunnlag
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.StansOpphørRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.UføreSøknadVedtakResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.integrasjon.createGatewayProvider
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.FakeUnleashBaseWithDefaultDisabled
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.inMemoryRepositoryProvider
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.temporal.ChronoUnit
import java.util.stream.Stream
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class BrevUtlederServiceTest {
    val repositoryProvider = inMemoryRepositoryProvider
    val gatewayProvider = createGatewayProvider {
        register<BrevUtlederServiceTestUnleash>()
    }

    val tilkjentYtelseRepository = repositoryProvider.provide<TilkjentYtelseRepository>()
    val vedtakRepository = repositoryProvider.provide<VedtakRepository>()
    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
    val beregningsgrunnlagRepository = repositoryProvider.provide<BeregningsgrunnlagRepository>()
    val beregningVurderingRepository = repositoryProvider.provide<BeregningVurderingRepository>()
    val aktivitetspliktRepository = repositoryProvider.provide<Aktivitetsplikt11_7Repository>()
    val arbeidsopptrappingRepository = repositoryProvider.provide<ArbeidsopptrappingRepository>()
    val sykdomsvurderingForBrevRepository = repositoryProvider.provide<SykdomsvurderingForBrevRepository>()
    val underveisRepository = repositoryProvider.provide<UnderveisRepository>()
    val overgangUføreRepository = repositoryProvider.provide<OvergangUføreRepository>()
    val unleashGateway = BrevUtlederServiceTestUnleash
    val stansOpphørRepository = repositoryProvider.provide<StansOpphørRepository>()

    val brevUtlederService = BrevUtlederService(
        repositoryProvider,
        gatewayProvider
    )

    val virkningstidspunkt = 1 januar 2025

    @Nested
    inner class TestGruppe_UtvidVedtakslengde {

        @Test
        fun `utledBehov legger ved sisteDagMedYtelse & fomDato i faktagrunnlag for UtvidVedtakslengde brev ved revurdering`() {
            val sisteDagFørstegang = 31 august 2025
            val sisteDagRevurdering = 31 desember 2025
            val revurdering = gittRevurderingForUtvidetVedtakslengde(
                sisteDagMedYtelseFørstegangsbehandling = sisteDagFørstegang,
                sisteDagMedYtelseRevurdering = sisteDagRevurdering,
            )

            val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id)

            assertIs<UtvidVedtakslengde>(resultat, "forventer brevbehov er av typen UtvidVedtakslengdeBrev")
            assertEquals(TypeBrev.VEDTAK_UTVID_VEDTAKSLENGDE, resultat.vedtakslengdeTypeBrev)
            assertEquals(sisteDagRevurdering, resultat.sisteDagMedYtelse)
            assertEquals(sisteDagFørstegang.plusDays(1), resultat.utvidetAapFomDato)
        }

        @ParameterizedTest(name = "skal utlede brevtype {1} for avslagsårsak {0}")
        @MethodSource("no.nav.aap.behandlingsflyt.behandling.brev.BrevUtlederServiceTest#avslagsårsakTilBrevBehov")
        fun `skal utlede riktig brevbehov for avslagsårsak ved utvidelse under ett år`(
            avslagsårsak: Avslagsårsak,
            forventetTypeBrev: TypeBrev
        ) {
            val revurdering = gittRevurderingForUtvidetVedtakslengde(
                avslagsårsak = setOf(avslagsårsak),
            )

            val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id)

            assertIs<UtvidVedtakslengde>(resultat)
            assertEquals(forventetTypeBrev, resultat.vedtakslengdeTypeBrev)
        }

        @Test
        @Disabled("MANGLENDE_DOKUMENTASJON er ikke en avslagsårsak som kan oppstå.")
        fun `skal feile ved ustøttet avslagsårsak ved utvidelse under ett år`() {
            val revurdering = gittRevurderingForUtvidetVedtakslengde(
                avslagsårsak = setOf(Avslagsårsak.MANGLENDE_DOKUMENTASJON)
            )

            assertThrows<IllegalArgumentException> {
                brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id)
            }
        }

        @Test
        fun `skal prioritere avslagsårsak med høyest prioritet ved flere avslagsårsaker`() {
            val revurdering = gittRevurderingForUtvidetVedtakslengde(
                avslagsårsak = setOf(
                    Avslagsårsak.ANNEN_FULL_YTELSE,
                    Avslagsårsak.BRUKER_OVER_67
                )
            )

            val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id)

            assertIs<UtvidVedtakslengde>(resultat)
            assertEquals(TypeBrev.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_4, resultat.vedtakslengdeTypeBrev)
        }

        private fun gittRevurderingForUtvidetVedtakslengde(
            sisteDagMedYtelseFørstegangsbehandling: LocalDate = 31 august 2025,
            sisteDagMedYtelseRevurdering: LocalDate = 31 desember 2025,
            avslagsårsak: Set<Avslagsårsak>? = null,
        ): Behandling {
            val førstegangsbehandling = gittBehandling(
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                status = Status.AVSLUTTET,
                årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
                vurderingsbehov = emptyList()
            )
            gittDynamiskUnderveisGrunnlag(
                behandlingId = førstegangsbehandling.id,
                sisteDagMedYtelse = sisteDagMedYtelseFørstegangsbehandling,
                rettighetsType = RettighetsType.BISTANDSBEHOV
            )

            val revurdering = gittBehandling(
                typeBehandling = TypeBehandling.Revurdering,
                status = Status.OPPRETTET,
                årsakTilOpprettelse = ÅrsakTilOpprettelse.UTVID_VEDTAKSLENGDE,
                vurderingsbehov = listOf(Vurderingsbehov.UTVID_VEDTAKSLENGDE),
                forrigeBehandlingId = førstegangsbehandling.id
            )
            gittDynamiskUnderveisGrunnlag(
                behandlingId = revurdering.id,
                førsteDagMedYtelse = sisteDagMedYtelseFørstegangsbehandling.plusDays(1),
                sisteDagMedYtelse = sisteDagMedYtelseRevurdering,
                rettighetsType = RettighetsType.BISTANDSBEHOV
            )

            if (avslagsårsak != null) {
                stansOpphørRepository.lagre(
                    revurdering.id, StansOpphørGrunnlag(
                        setOf(
                            GjeldendeStansEllerOpphør(
                                fom = sisteDagMedYtelseRevurdering.plusDays(1),
                                opprettet = Instant.now(),
                                vurdertIBehandling = revurdering.id,
                                vurdering = StansEllerOpphør.fraÅrsaker(avslagsårsak),
                            )
                        )
                    )
                )
            }

            return revurdering
        }
    }

    @Nested
    inner class TestGruppe_Arbeidssøkerbrev_11_17 {
        @Test
        fun `utledBehov legger ved sisteDagMedYtelse & datoAvklartForJobbsøk i faktagrunnlag for 11-17 brev ved revurdering`() {
            val førstegangsbehandling = gittBehandling(
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                status = Status.AVSLUTTET,
                årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
                vurderingsbehov = emptyList()
            )

            gittUnderveisGrunnlag(
                førstegangsbehandling.id, underveisperiode(
                    periode = Periode(1 januar 2025, 31 august 2025),
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    utfall = Utfall.OPPFYLT,
                )
            )

            val revurdering = gittBehandling(
                typeBehandling = TypeBehandling.Revurdering,
                status = Status.OPPRETTET,
                årsakTilOpprettelse = ÅrsakTilOpprettelse.HELSEOPPLYSNINGER,
                vurderingsbehov = listOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
                forrigeBehandlingId = førstegangsbehandling.id
            )
            val datoAvklartForJobbsøk = 2 januar 2025
            val sisteDagMedYtelse = 31 august 2025
            gittUnderveisGrunnlag(
                revurdering.id,
                underveisperiode(
                    periode = Periode(datoAvklartForJobbsøk, sisteDagMedYtelse),
                    rettighetsType = RettighetsType.ARBEIDSSØKER,
                    utfall = Utfall.OPPFYLT,
                )
            )

            val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id)

            assertIs<Arbeidssøker>(resultat, "forventer brevbehov er av typen Arbeidssøker (11-17)")
            assertEquals(sisteDagMedYtelse, resultat.sisteDagMedYtelse)
            assertEquals(datoAvklartForJobbsøk, resultat.datoAvklartForJobbsøk)
        }

        @Test
        fun `skal utlede brev etter rettighetstype § 11-17 ved innvilgelse av revurdering`() {
            val førstegangsbehandling = gittBehandling(TypeBehandling.Førstegangsbehandling)
            gittUnderveisGrunnlag(
                førstegangsbehandling.id,
                underveisperiode(
                    periode = Periode(1 januar 2025, 31 desember 2025),
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    utfall = Utfall.IKKE_OPPFYLT,
                )
            )
            val revurdering = gittBehandling(
                typeBehandling = TypeBehandling.Revurdering,
                forrigeBehandlingId = førstegangsbehandling.id,
                vurderingsbehov = listOf(Vurderingsbehov.OVERGANG_ARBEID)
            )
            gittUnderveisGrunnlag(
                førstegangsbehandling.id,
                underveisperiode(
                    periode = Periode(1 januar 2023, 31 desember 2023),
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    utfall = Utfall.OPPFYLT,
                )
            )
            gittUnderveisGrunnlag(
                førstegangsbehandling.id,
                underveisperiode(
                    periode = Periode(1 januar 2023, 31 desember 2023),
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    utfall = Utfall.OPPFYLT,
                )
            )
            gittUnderveisGrunnlag(
                revurdering.id,
                underveisperiode(
                    periode = Periode(1 januar 2023, 28 februar 2023),
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    utfall = Utfall.OPPFYLT,
                ),
                underveisperiode(
                    periode = Periode(1 mars 2023, 31 desember 2023),
                    rettighetsType = RettighetsType.ARBEIDSSØKER,
                    utfall = Utfall.OPPFYLT,
                )
            )

            val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id)

            assertIs<Arbeidssøker>(resultat, "brevbehov er av type Arbeidssøker")
        }
    }

    @Nested
    inner class TestGruppe_VurderesForUføreBrev_11_18 {
        @Test
        fun `skal utlede brev etter rettighetstype § 11-18 ved førstegangsbehandling`() {
            val kravdatoUføretrygd = 20 februar 2023
            val behandling = gittBehandling(TypeBehandling.Førstegangsbehandling)
            val førsteDagMedYtelse = 1 januar 2025
            val sisteDagMedYtelse = 31 august 2025
            gittUnderveisGrunnlag(
                behandling.id,
                underveisperiode(
                    periode = Periode(førsteDagMedYtelse, sisteDagMedYtelse),
                    rettighetsType = RettighetsType.VURDERES_FOR_UFØRETRYGD,
                    utfall = Utfall.OPPFYLT,
                ),
            )
            overgangUføreRepository.lagre(behandling.id,
                listOf(
                    OvergangUføreVurdering(
                        begrunnelse = "test",
                        brukerHarSøktOmUføretrygd = true,
                        brukerHarFåttVedtakOmUføretrygd = UføreSøknadVedtakResultat.NEI,
                        brukerRettPåAAP = true,
                        fom = kravdatoUføretrygd,
                        tom = sisteDagMedYtelse,
                        vurdertAv = "meg",
                        vurdertIBehandling = behandling.id,
                    )
                )
            )

            val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(behandling.id)

            assertIs<VurderesForUføretrygd>(resultat, "forventer brevbehov er av typen 11-18")
            assertEquals(sisteDagMedYtelse, resultat.sisteDagMedYtelse)
            assertEquals(kravdatoUføretrygd, resultat.kravdatoUføretrygd)
        }

        @Test
        fun `skal utlede brev etter rettighetstype § 11-18 ved revurdering`() {
            val førstegangsbehandling = gittBehandling(TypeBehandling.Førstegangsbehandling)
            val revurdering = gittBehandling(
                typeBehandling = TypeBehandling.Revurdering,
                forrigeBehandlingId = førstegangsbehandling.id,
                vurderingsbehov = listOf(Vurderingsbehov.OVERGANG_UFORE)
            )
            gittUnderveisGrunnlag(
                førstegangsbehandling.id,
                underveisperiode(
                    periode = Periode(1 januar 2023, 31 desember 2023),
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    utfall = Utfall.OPPFYLT,
                )
            )
            val sisteDagMedYtelse = 31 desember 2023
            gittUnderveisGrunnlag(
                revurdering.id,
                underveisperiode(
                    periode = Periode(1 januar 2023, 28 februar 2023),
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    utfall = Utfall.OPPFYLT,
                ),
                underveisperiode(
                    periode = Periode(1 mars 2023, sisteDagMedYtelse),
                    rettighetsType = RettighetsType.VURDERES_FOR_UFØRETRYGD,
                    utfall = Utfall.OPPFYLT,
                )
            )
            beregningsgrunnlagRepository.lagre(revurdering.id, Grunnlag11_19(
                    grunnlaget = GUnit(2),
                    erGjennomsnitt = false,
                    gjennomsnittligInntektIG = GUnit(0),
                    inntekter = listOf(
                        grunnlagInntekt(2024, 220_000),
                        grunnlagInntekt(2023, 210_000),
                        grunnlagInntekt(2022, 200_000),
                    )
                )
            )
            val kravdatoUføretrygd = LocalDate.of(2023, 2, 20)
            beregningVurderingRepository.lagre(revurdering.id,
                BeregningstidspunktVurdering(
                    begrunnelse = "",
                    nedsattArbeidsevneEllerStudieevneDato = kravdatoUføretrygd,
                    ytterligereNedsattBegrunnelse = null,
                    ytterligereNedsattArbeidsevneDato = null,
                    vurdertAv = ""
                )
            )
            vedtakRepository.lagre(revurdering.id,
                LocalDateTime.now(),
                kravdatoUføretrygd
            )
            overgangUføreRepository.lagre(revurdering.id,
                listOf(
                    OvergangUføreVurdering(
                        begrunnelse = "test",
                        brukerHarSøktOmUføretrygd = true,
                        brukerHarFåttVedtakOmUføretrygd = UføreSøknadVedtakResultat.NEI,
                        brukerRettPåAAP = true,
                        fom = kravdatoUføretrygd,
                        tom = sisteDagMedYtelse,
                        vurdertAv = "meg",
                        vurdertIBehandling = revurdering.id,
                    )
                )
            )

            assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id)).isEqualTo(
                VurderesForUføretrygd(
                    kravdatoUføretrygd = kravdatoUføretrygd,
                    sisteDagMedYtelse = sisteDagMedYtelse,
                    grunnlagBeregning = GrunnlagBeregning(
                        beregningstidspunkt = LocalDate.of(2023, 2, 20),
                        inntekterPerÅr = listOf(
                            InntektPerÅr(Year.of(2024), inntekt = BigDecimal("220000.00")),
                            InntektPerÅr(Year.of(2023), inntekt = BigDecimal("210000.00")),
                            InntektPerÅr(Year.of(2022), inntekt = BigDecimal("200000.00")),
                        ),
                        beregningsgrunnlag = null,
                        beregningsutfallKategori = GrunnlagBeregning.BeregningsutfallKategori.SISTE_AAR
                    ),
                    tilkjentYtelse = null
                )
            )
        }
    }

    @Nested
    inner class TestGruppe_VedtakEndring {

        @Test
        fun `skal ikke utlede brev etter rettighetstype § 11-18 ved innvilgelse av revurdering dersom det samme gjelder forrige behandling`() {
            val førstegangsbehandling = gittBehandling(TypeBehandling.Førstegangsbehandling)
            val revurdering = gittBehandling(
                typeBehandling = TypeBehandling.Revurdering,
                forrigeBehandlingId = førstegangsbehandling.id,
                vurderingsbehov = listOf(Vurderingsbehov.OVERGANG_UFORE)
            )
            gittUnderveisGrunnlag(
                førstegangsbehandling.id,
                underveisperiode(
                    periode = Periode(1 januar 2023, 31 desember 2023),
                    rettighetsType = RettighetsType.VURDERES_FOR_UFØRETRYGD,
                    utfall = Utfall.OPPFYLT,
                )
            )
            gittUnderveisGrunnlag(
                revurdering.id,
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

        @Test
        fun `skal ikke utlede brev etter rettighetstype § 11-17 ved innvilgelse av revurdering dersom det samme gjelder forrige behandling`() {
            val forrigeBehandling = gittBehandling(typeBehandling = TypeBehandling.Revurdering)
            val revurdering = gittBehandling(
                typeBehandling = TypeBehandling.Revurdering,
                forrigeBehandlingId = forrigeBehandling.id,
                vurderingsbehov = listOf(Vurderingsbehov.OVERGANG_ARBEID)
            )
            gittUnderveisGrunnlag(
                forrigeBehandling.id,
                underveisperiode(
                    periode = Periode(1 januar 2023, 31 desember 2023),
                    rettighetsType = RettighetsType.ARBEIDSSØKER,
                    utfall = Utfall.OPPFYLT,
                )
            )
            gittUnderveisGrunnlag(
                revurdering.id,
                underveisperiode(
                    periode = Periode(1 januar 2023, 31 desember 2023),
                    rettighetsType = RettighetsType.ARBEIDSSØKER,
                    utfall = Utfall.OPPFYLT,
                )
            )

            assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id)).isEqualTo(VedtakEndring)
        }

        @Test
        fun `skal utlede brevtype VedtakEndring når et aktivitetsplikt brudd omgjøres`() {
            val aktivitetspliktBehandling = gittBehandling(
                typeBehandling = TypeBehandling.Aktivitetsplikt,
                årsakTilOpprettelse = ÅrsakTilOpprettelse.AKTIVITETSPLIKT,
            )
            aktivitetspliktRepository.lagre(aktivitetspliktBehandling.id,
                listOf(aktivitetspliktBruddOppfylt(aktivitetspliktBehandling.id))
            )

            assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(aktivitetspliktBehandling.id)).isEqualTo(
                VedtakEndring
            )
        }

        @Test
        fun `skal utlede brevtype VedtakEndring når nyeste aktivitetsplikt brudd omgjøres`() {
            val aktivitetspliktBehandling = gittBehandling(
                typeBehandling = TypeBehandling.Aktivitetsplikt,
                årsakTilOpprettelse = ÅrsakTilOpprettelse.AKTIVITETSPLIKT,
            )

            val gammelBruddDato = LocalDate.now().minusDays(100)
            aktivitetspliktRepository.lagre(aktivitetspliktBehandling.id,
                listOf(
                    aktivitetspliktBrudd(BehandlingId(100), fra = gammelBruddDato),
                    aktivitetspliktBrudd(BehandlingId(101), fra = gammelBruddDato.plusDays(1)),
                    aktivitetspliktBrudd(BehandlingId(102)),
                    aktivitetspliktBruddOppfylt(aktivitetspliktBehandling.id)
                )
            )

            assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(aktivitetspliktBehandling.id)).isEqualTo(
                VedtakEndring
            )
        }

    }

    @Nested
    inner class TestGruppe_InnvilgelseBrev {
        @Test
        fun `utledBehov legger ved sisteDagMedYtelse i faktagrunnlag for Innvilgelse-brev`() {
            val behandling = gittBehandling(TypeBehandling.Førstegangsbehandling)
            val førsteDagMedYtelse = 1 januar 2025
            val sisteDagMedYtelse = 31 august 2025
            gittUnderveisGrunnlag(
                behandling.id,
                underveisperiode(
                    periode = Periode(førsteDagMedYtelse, sisteDagMedYtelse),
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    utfall = Utfall.OPPFYLT,
                ),
            )

            val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(behandling.id)

            assertIs<Innvilgelse>(resultat, "forventer brevbehov er av typen Innvilgelse")
            assertEquals(sisteDagMedYtelse, resultat.sisteDagMedYtelse)
        }

        @Test
        fun `utledBehov legger ved 3 alternative beløp for årlig ytelse i faktagrunnlag for Innvilgelse-brev`() {
            val behandling = gittBehandling(TypeBehandling.Førstegangsbehandling)
            gittUnderveisGrunnlag(
                behandling.id,
                underveisperiode(
                    periode = Periode(1 januar 2025, 31 august 2025),
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    utfall = Utfall.OPPFYLT,
                ),
            )
            val dagsats = Beløp("1000.00")
            tilkjentYtelseRepository.lagre(behandling.id,
                stubTilkjentYtelse(dagsats),
                mockk<TilkjentYtelseGrunnlag>(),
                ""
            )

            val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(behandling.id)

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
            val behandling = gittBehandling(TypeBehandling.Førstegangsbehandling)
            gittUnderveisGrunnlag(
                behandling.id,
                underveisperiode(
                    periode = Periode(1 januar 2025, 31 august 2025),
                    rettighetsType = RettighetsType.BISTANDSBEHOV,
                    utfall = Utfall.OPPFYLT,
                ),
            )
            sykdomsvurderingForBrevRepository.lagre(behandling.id, sykdomsvurderingForBrevGrunnlag())

            val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(behandling.id)

            assertIs<Innvilgelse>(resultat, "brevbehov er av type Innvilgelse")
            assertEquals(resultat.sykdomsvurdering, "Vurdering av sykdom")
        }

        @Test
        fun `skal få innvilgelsesbrev etter avslag`() {
            val førstegangsbehandling = gittBehandling()
            val revurdering = gittBehandling(forrigeBehandlingId = førstegangsbehandling.id)

            gittUnderveisGrunnlag(
                førstegangsbehandling.id,
                underveisperiode(
                    periode = Periode(virkningstidspunkt, virkningstidspunkt.plusYears(1)),
                    utfall = Utfall.IKKE_OPPFYLT,
                    rettighetsType = null
                ),
            )

            gittUnderveisGrunnlag(
                revurdering.id,
                underveisperiode(
                    periode = Periode(virkningstidspunkt, virkningstidspunkt.plusDays(14)),
                    utfall = Utfall.IKKE_OPPFYLT,
                    rettighetsType = null,
                ),
                underveisperiode(
                    periode = Periode(virkningstidspunkt.plusDays(15), virkningstidspunkt.plusYears(1)),
                    utfall = Utfall.OPPFYLT,
                    rettighetsType = RettighetsType.BISTANDSBEHOV
                ),
            )

            val resultatFørstegangsbehandling = brevUtlederService.utledBehovForMeldingOmVedtak(førstegangsbehandling.id)
            val resultatAndreBehandling = brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id)

            assertIs<Avslag>(resultatFørstegangsbehandling, "første behandling gir avslag")
            assertIs<Innvilgelse>(resultatAndreBehandling, "revurdering er innvilgelse")
        }
    }


    @Nested
    inner class TestGruppe_AvslagBrev {

        @Test
        fun `skal hente ut sykdomsvurdering ved avslag`() {
            val behandling = gittBehandling(TypeBehandling.Førstegangsbehandling)
            gittUnderveisgrunnlagAvslag(behandling.id)
            sykdomsvurderingForBrevRepository.lagre(behandling.id, sykdomsvurderingForBrevGrunnlag())

            val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(behandling.id)

            assertIs<Avslag>(resultat, "brevbehov er av type Avslag")
            assertEquals(resultat.sykdomsvurdering, "Vurdering av sykdom")
        }

        @Test
        fun `faktagrunnlag for sykdomsvurdering settes til null hvis det ikke finnes`() {
            val behandling = gittBehandling(TypeBehandling.Førstegangsbehandling)
            gittUnderveisgrunnlagAvslag(behandling.id)

            val resultat = brevUtlederService.utledBehovForMeldingOmVedtak(behandling.id)

            assertIs<Avslag>(resultat, "brevbehov er av type Avslag")
            assertEquals(null, resultat.sykdomsvurdering)
        }
    }

    @Nested
    inner class TestGruppe_VedtakAktivitetsplikt {

        @Test
        fun `skal feile ved utleding av brevtype dersom aktivitetsplikt mangler for aktivitetsplikt-behandling`() {
            val aktivitetspliktBehandling = gittBehandling(
                typeBehandling = TypeBehandling.Aktivitetsplikt,
                årsakTilOpprettelse = ÅrsakTilOpprettelse.AKTIVITETSPLIKT,
            )
            assertThrows<IllegalStateException> {
                brevUtlederService.utledBehovForMeldingOmVedtak(aktivitetspliktBehandling.id)
            }
        }

        @Test
        fun `skal utlede brevtype VedtakAktivitetsplikt11_7 når det iverksettes brudd på 11_7`() {
            val aktivitetspliktBehandling = gittBehandling(
                typeBehandling = TypeBehandling.Aktivitetsplikt,
                årsakTilOpprettelse = ÅrsakTilOpprettelse.AKTIVITETSPLIKT,
            )

            aktivitetspliktRepository.lagre(aktivitetspliktBehandling.id,
                listOf(aktivitetspliktBrudd(aktivitetspliktBehandling.id))
            )

            assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(aktivitetspliktBehandling.id)).isEqualTo(
                VedtakAktivitetsplikt11_7
            )
        }

        @Test
        fun `skal utlede brevtype Aktivitetspliktbrudd når nyeste aktivitetsplikt brudd er stans`() {
            val aktivitetspliktBehandling = gittBehandling(
                typeBehandling = TypeBehandling.Aktivitetsplikt,
                årsakTilOpprettelse = ÅrsakTilOpprettelse.AKTIVITETSPLIKT,
            )

            val gammelBruddDato = LocalDate.now().minusDays(100)
            aktivitetspliktRepository.lagre(aktivitetspliktBehandling.id,
                listOf(
                    aktivitetspliktBrudd(BehandlingId(100), fra = gammelBruddDato),
                    aktivitetspliktBrudd(BehandlingId(101), fra = gammelBruddDato.plusDays(1)),
                    aktivitetspliktBruddOppfylt(BehandlingId(102)),
                    aktivitetspliktBrudd(aktivitetspliktBehandling.id)
                )
            )

            assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(aktivitetspliktBehandling.id)).isEqualTo(
                VedtakAktivitetsplikt11_7
            )
        }
    }


    @Nested
    inner class TestGruppe_VedtakArbeidsopptrapping11_23_sjetteLedd {

        @Test
        fun `skal utlede brev for § 11-23 sjette ledd ved arbeidsopptrapping på gjeldende behandling og ikke på forrige behandling`() {
            val revurdering = gittBehandling(
                typeBehandling = TypeBehandling.Revurdering,
                vurderingsbehov = listOf(Vurderingsbehov.OVERGANG_ARBEID)
            )
            arbeidsopptrappingRepository.lagre(revurdering.id, arbeidsopptrappingGrunnlag(
                revurdering,
                1 januar 2024,
                31 desember 2024
            ))
            gittUnderveisGrunnlag(
                revurdering.id,
                underveisperiode(
                    periode = Periode(1 januar 2024, 31 desember 2024),
                    rettighetsType = RettighetsType.ARBEIDSSØKER,
                    utfall = Utfall.OPPFYLT,
                )
            )

            assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id))
                .isEqualTo(VedtakArbeidsopptrapping11_23SjetteLedd)
        }

        @Test
        fun `skal ikke utlede brev for § 11-23 sjette ledd ved arbeidsopptrapping på gjeldende behandling i tillegg til forrige behandling`() {
            val revurdering = gittBehandling(
                typeBehandling = TypeBehandling.Revurdering,
                vurderingsbehov = listOf(Vurderingsbehov.OVERGANG_ARBEID)
            )
            arbeidsopptrappingRepository.lagre(revurdering.id, arbeidsopptrappingGrunnlag(
                    revurdering,
                    1 januar 2024,
                    31 desember 2024
                )
            )
            arbeidsopptrappingRepository.lagre(revurdering.forrigeBehandlingId!!, arbeidsopptrappingGrunnlag(
                    revurdering,
                    1 januar 2023,
                    31 desember 2023
                )
            )
            gittUnderveisGrunnlag(
                revurdering.id,
                underveisperiode(
                    periode = Periode(1 januar 2024, 31 desember 2024),
                    rettighetsType = RettighetsType.ARBEIDSSØKER,
                    utfall = Utfall.OPPFYLT,
                )
            )
            gittUnderveisGrunnlag(
                revurdering.forrigeBehandlingId!!,
                underveisperiode(
                    periode = Periode(1 januar 2023, 31 desember 2023),
                    rettighetsType = RettighetsType.ARBEIDSSØKER,
                    utfall = Utfall.OPPFYLT,
                )
            )

            assertThat(brevUtlederService.utledBehovForMeldingOmVedtak(revurdering.id))
                .isNotEqualTo(VedtakArbeidsopptrapping11_23SjetteLedd)
        }
    }

    private fun arbeidsopptrappingGrunnlag(
        revurdering: Behandling,
        fraDato: LocalDate,
        tilDato: LocalDate
    ) = listOf(
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

    private fun gittBehandling(
        typeBehandling: TypeBehandling? = null,
        forrigeBehandlingId: BehandlingId? = null,
        sakId: SakId = SakId(Random.nextLong()),
        status: Status = Status.OPPRETTET,
        årsakTilOpprettelse: ÅrsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
        vurderingsbehov: List<Vurderingsbehov> = listOf(Vurderingsbehov.MOTTATT_SØKNAD)
    ): Behandling {
        val typeBehandling = typeBehandling
            ?: if (forrigeBehandlingId == null) TypeBehandling.Førstegangsbehandling else TypeBehandling.Revurdering
        val forrigeBehandlingId = forrigeBehandlingId
            ?: if (typeBehandling == TypeBehandling.Revurdering) gittBehandling().id else null

        val behandling = behandlingRepository.opprettBehandling(
            sakId = sakId,
            typeBehandling = typeBehandling,
            forrigeBehandlingId = forrigeBehandlingId,
            vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                vurderingsbehov = vurderingsbehov.map { VurderingsbehovMedPeriode(it) },
                årsak = årsakTilOpprettelse,
            )
        )
        behandlingRepository.oppdaterBehandlingStatus(behandling.id, status)
        vedtakRepository.lagre(
            behandlingId = behandling.id,
            vedtakstidspunkt = virkningstidspunkt.atStartOfDay(),
            virkningstidspunkt = virkningstidspunkt,
        )

        return behandling
    }

    private fun gittUnderveisGrunnlag(
        behandlingId: BehandlingId,
        vararg underveisperioder: Underveisperiode
    ): UnderveisGrunnlag {
        underveisRepository.lagre(
            behandlingId,
            underveisperioder.toList(),
            object : Faktagrunnlag {},
        )

        return underveisRepository.hent(behandlingId)
    }

    private fun underveisperiode(
        periode: Periode,
        rettighetsType: RettighetsType?,
        utfall: Utfall,
    ): Underveisperiode {
        return Underveisperiode(
            periode = periode,
            meldePeriode = periode,
            utfall = utfall,
            rettighetsType = rettighetsType,
            avslagsårsak = when (utfall) {
                Utfall.OPPFYLT, Utfall.IKKE_VURDERT, Utfall.IKKE_RELEVANT -> null
                Utfall.IKKE_OPPFYLT -> UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT
            },
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

    private fun gittDynamiskUnderveisGrunnlag(
        behandlingId: BehandlingId,
        førsteDagMedYtelse: LocalDate = 1 januar 2025,
        sisteDagMedYtelse: LocalDate = 31 august 2025,
        rettighetsType: RettighetsType = RettighetsType.BISTANDSBEHOV,
    ): UnderveisGrunnlag {
        val antallDager = ChronoUnit.DAYS.between(førsteDagMedYtelse, sisteDagMedYtelse)
        val midtMellomDag = førsteDagMedYtelse.plusDays(antallDager / 2)
        return gittUnderveisGrunnlag(
            behandlingId,
            underveisperiode(
                periode = Periode(førsteDagMedYtelse, midtMellomDag),
                rettighetsType = rettighetsType,
                utfall = Utfall.OPPFYLT,
            ),
            underveisperiode(
                periode = Periode(midtMellomDag, sisteDagMedYtelse),
                rettighetsType = rettighetsType,
                utfall = Utfall.OPPFYLT,
            ),
            underveisperiode(
                periode = Periode(sisteDagMedYtelse.plusDays(1), sisteDagMedYtelse.plusMonths(4)),
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

    private fun gittUnderveisgrunnlagAvslag(behandlingId: BehandlingId) {
        gittUnderveisGrunnlag(
            behandlingId,
            underveisperiode(
                periode = Periode(1 januar 2025, 31 desember 2025),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                utfall = Utfall.IKKE_OPPFYLT,
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
            barnepensjonDagsats = Beløp(0),
            antallBarn = 0,
            barnetilleggsats = Beløp(0),
            barnetillegg = Beløp(0),
            utbetalingsdato = utbetalingsdato,
            minsteSats = Minstesats.IKKE_MINSTESATS,
            redusertDagsats = dagsats
        )
    }

    companion object {
        @JvmStatic
        fun avslagsårsakTilBrevBehov(): Stream<Arguments> = Stream.of(
            Arguments.of(Avslagsårsak.BRUKER_OVER_67, TypeBrev.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_4),
            Arguments.of(Avslagsårsak.IKKE_MEDLEM, TypeBrev.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_MEDLEMSKAP),
            Arguments.of(Avslagsårsak.ORDINÆRKVOTE_BRUKT_OPP, TypeBrev.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_12),
            Arguments.of(Avslagsårsak.BRUDD_PÅ_OPPHOLDSKRAV_STANS, TypeBrev.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_3),
            Arguments.of(
                Avslagsårsak.IKKE_RETT_UNDER_STRAFFEGJENNOMFØRING,
                TypeBrev.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_26
            ),
            Arguments.of(Avslagsårsak.ANNEN_FULL_YTELSE, TypeBrev.VEDTAK_FORLENGELSE_UNDER_ETT_ÅR_11_27),
        )
    }
}

object BrevUtlederServiceTestUnleash : FakeUnleashBaseWithDefaultDisabled(
    enabledFlags = listOf(BehandlingsflytFeature.SamordningFaktagrunnlagBrev)
)
