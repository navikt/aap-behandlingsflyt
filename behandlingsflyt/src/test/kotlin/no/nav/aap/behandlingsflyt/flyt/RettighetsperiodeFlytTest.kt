package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarPeriodisertLovvalgMedlemskapLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.tilTidslinje
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLandEllerLandMedAvtale
import no.nav.aap.behandlingsflyt.drift.Driftfunksjoner
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.PeriodisertManuellVurderingForLovvalgMedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.SamordningVurderingData
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.VurderingerForSamordning
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`100_PROSENT`
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.reflect.KClass


@ParameterizedClass
@MethodSource("unleashTestDataSource")
class RettighetsperiodeFlytTest(val unleashGateway: KClass<UnleashGateway>) :
    AbstraktFlytOrkestratorTest(unleashGateway) {

    @Test
    fun `Skal kunne overstyre rettighetsperioden på en revurdering - innskrenke perioden`() {
        val sak = happyCaseFørstegangsbehandling(LocalDate.now())
        val ident = sak.person.aktivIdent()
        val førsteOverstyring = sak.rettighetsperiode.fom.minusMonths(2)
        val andreOverstyring = sak.rettighetsperiode.fom.minusMonths(1)

        /**
         * Utvid rettighetsperioden
         */
        val avklaringsbehovManuellRevurdering =
            listOf(Vurderingsbehov.VURDER_RETTIGHETSPERIODE)
        sak.opprettManuellRevurdering(avklaringsbehovManuellRevurdering)
            .medKontekst {
                assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }.løsRettighetsperiode(førsteOverstyring)
            .løsSykdom(førsteOverstyring)
            .løsBistand(førsteOverstyring)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .løsBeregningstidspunkt(LocalDate.now())
            .løsOppholdskrav(førsteOverstyring)
            .løsUtenSamordning()
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)
                assertThat(åpneAvklaringsbehov).anySatisfy { assertTrue(it.definisjon == Definisjon.SKRIV_VEDTAKSBREV) }
            }
            .løsVedtaksbrev(TypeBrev.VEDTAK_ENDRING)
            .medKontekst {
                val oppdatertRettighetsperiode = hentSak(ident, sak.rettighetsperiode).rettighetsperiode
                assertThat(oppdatertRettighetsperiode).isEqualTo(Periode(førsteOverstyring, Tid.MAKS))
            }

        /**
         * Innskrenke rettighetsperioden, men ikke etter søknadsdato
         */
        val revurderingInnskrenking = sak.opprettManuellRevurdering(avklaringsbehovManuellRevurdering)
            .medKontekst {
                assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }

        /**
         * Innskrenker perioden til etter søknadsdato,
         */
        val feil = assertThrows<UgyldigForespørselException> {
            revurderingInnskrenking.løsRettighetsperiode(LocalDate.now().plusDays(1))
        }
        assertThat(feil.message).contains("Kan ikke endre starttidspunkt til å gjelde ETTER søknadstidspunkt")

        revurderingInnskrenking
            .løsRettighetsperiode(andreOverstyring)
            .løsBeregningstidspunkt(LocalDate.now())
            .løsUtenSamordning()
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)
                assertThat(åpneAvklaringsbehov).anySatisfy { assertTrue(it.definisjon == Definisjon.SKRIV_VEDTAKSBREV) }
            }
            .løsVedtaksbrev(TypeBrev.VEDTAK_ENDRING)

        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(revurderingInnskrenking.id)
        assertThat(åpneAvklaringsbehov).isEmpty()

        val oppdatertSak = hentSak(ident, sak.rettighetsperiode)

        assertThat(oppdatertSak.rettighetsperiode).isNotEqualTo(sak.rettighetsperiode)
        assertThat(oppdatertSak.rettighetsperiode).isEqualTo(
            Periode(
                andreOverstyring,
                sak.rettighetsperiode.tom
            )
        )
    }

    @Test
    fun `skal kunne gjennomføre førstegangsbehandling og revurdering hvor rettighetsperiode endres og fører til avklaringsbehov for lovvalg og medlemskap`() {
        var (sak, førstegangsbehandling) = sendInnFørsteSøknad(søknad = TestSøknader.SØKNAD_INGEN_MEDLEMSKAP)

        førstegangsbehandling = førstegangsbehandling
            .løsAvklaringsBehov(
                AvklarPeriodisertLovvalgMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = sak.rettighetsperiode.fom,
                            tom = null,
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
                            medlemskap = MedlemskapDto("begrunnelse", true)
                        )
                    )
                )
            )
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand(sak.rettighetsperiode.fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap(sak.rettighetsperiode.fom)
            .løsOppholdskrav(sak.rettighetsperiode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()

        assertThat(førstegangsbehandling.status()).isEqualTo(Status.AVSLUTTET)

        val nyStartDato = sak.rettighetsperiode.fom.minusMonths(1)
        val revurdering = sak.opprettManuellRevurdering(
            listOf(Vurderingsbehov.VURDER_RETTIGHETSPERIODE),
        )
            .løsRettighetsperiode(nyStartDato)
            .medKontekst {
                // Vi har ikke vurdert lovvalg og medlemskap for den utvidede perioden enda, så vi forventer et avklaringsbehov her
                assertThat(åpneAvklaringsbehov).anySatisfy { behov -> assertThat(behov.definisjon == Definisjon.AVKLAR_LOVVALG_MEDLEMSKAP).isTrue() }
            }
            .løsAvklaringsBehov(
                AvklarPeriodisertLovvalgMedlemskapLøsning(
                    løsningerForPerioder = listOf(
                        PeriodisertManuellVurderingForLovvalgMedlemskapDto(
                            fom = nyStartDato,
                            tom = null,
                            begrunnelse = "",
                            lovvalg = LovvalgDto("begrunnelse", EØSLandEllerLandMedAvtale.NOR),
                            medlemskap = MedlemskapDto("begrunnelse", true)
                        )
                    )
                )
            )
            .løsSykdom(nyStartDato)
            .løsBistand(nyStartDato)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap(nyStartDato)
            .løsOppholdskrav(nyStartDato)
            .løsUtenSamordning()
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_ENDRING)

        assertThat(revurdering.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Skal kunne overstyre rettighetsperioden og angre seg etterpå`() {
        val periode = Periode(LocalDate.now(), Tid.MAKS)
        val nyStartDato = periode.fom.minusDays(7)

        var (sak, behandling) = sendInnFørsteSøknad(
            mottattTidspunkt = periode.fom.atStartOfDay(),
            periode = periode,
        )

        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.AVKLAR_SYKDOM)

        behandling = sak.opprettManuellRevurdering(
            vurderingsbehov = listOf(Vurderingsbehov.VURDER_RETTIGHETSPERIODE),
        )

        behandling.løsRettighetsperiode(nyStartDato)

        assertThat(åpneAvklaringsbehov).hasSize(1).first().extracting(Avklaringsbehov::definisjon)
            .isEqualTo(Definisjon.AVKLAR_SYKDOM)

        hentSak(sak.saksnummer).also {
            assertThat(it.rettighetsperiode).isEqualTo(Periode(nyStartDato, Tid.MAKS))
        }

        behandling.løsRettighetsperiodeIngenEndring()
        hentSak(sak.saksnummer).also {
            assertThat(it.rettighetsperiode).isEqualTo(periode)
        }

    }

    @Test
    fun `Skal kunne overstyre rettighetsperioden i førstegangsbehandling hos NAY`() {
        val (sak, behandling) = sendInnFørsteSøknad()

        val ident = sak.person.aktivIdent()
        val nyStartDato = sak.rettighetsperiode.fom.minusDays(7)
        behandling
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand(sak.rettighetsperiode.fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .kvalitetssikre()

        var oppdatertBehandling = sak.opprettManuellRevurdering(
            listOf(Vurderingsbehov.VURDER_RETTIGHETSPERIODE),
        ).medKontekst {
            assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
            assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
        }

        oppdatertBehandling = oppdatertBehandling
            .løsRettighetsperiode(nyStartDato)
            .medKontekst {
                val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(oppdatertBehandling.id)
                assertThat(åpneAvklaringsbehov).hasSize(2)
                assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.AVKLAR_SYKDOM)

            }
            .løsSykdom(nyStartDato)
            .løsBistand(nyStartDato)
            .medKontekst {
                val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(oppdatertBehandling.id)
                assertThat(åpneAvklaringsbehov).hasSize(2)
                assertThat(åpneAvklaringsbehov).anySatisfy {
                    assertThat(it.definisjon).isEqualTo(Definisjon.SKRIV_SYKDOMSVURDERING_BREV)
                }

            }
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .medKontekst {
                val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(oppdatertBehandling.id)
                if (unleashGateway.objectInstance?.isEnabled(BehandlingsflytFeature.KvalitetssikringVed2213) == true) {
                    assertThat(åpneAvklaringsbehov).hasSize(2)
                    assertThat(åpneAvklaringsbehov).anySatisfy {
                        assertThat(it.definisjon).isEqualTo(Definisjon.KVALITETSSIKRING)
                    }
                } else {
                    assertThat(åpneAvklaringsbehov).hasSize(1)
                    assertThat(åpneAvklaringsbehov.first().definisjon).isEqualTo(Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT)
                }
            }
            .bekreftVurderinger()
            .kvalitetssikre()
            .løsBeregningstidspunkt(nyStartDato)
            .løsOppholdskrav(nyStartDato)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_INNVILGELSE)

        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(oppdatertBehandling.id)
        assertThat(åpneAvklaringsbehov).isEmpty()

        val oppdatertSak = hentSak(ident, sak.rettighetsperiode)

        val uthentetTilkjentYtelse =
            requireNotNull(dataSource.transaction { TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id) })
            { "Tilkjent ytelse skal være beregnet her." }


        assertThat(oppdatertSak.rettighetsperiode).isNotEqualTo(sak.rettighetsperiode)
        assertThat(oppdatertSak.rettighetsperiode).isEqualTo(
            Periode(
                nyStartDato,
                Tid.MAKS
            )
        )
        assertThat(uthentetTilkjentYtelse.tilTidslinje().helePerioden().fom).isEqualTo(nyStartDato)
    }

    @Test
    fun `Når rettighetsperioden flyttes tilbake etter innlevert meldekort skal Kelvin utbetale samme beløp for meldeperioden som er delvis levert`() {

        val søknadsdato = LocalDate.of(2026, 1, 8) // Torsdag
        val justertDato = LocalDate.of(2026, 1, 7) // Onsdag - utvides i samme meldeperiode

        val sak = happyCaseFørstegangsbehandling(søknadsdato, sendMeldekort = true)

        val (underveisGrunnlagFørEndring, tilkjentYtelseFørEndring) = dataSource.transaction {
            val repositoryProvider = postgresRepositoryRegistry.provider(it)
            val behandlingService = BehandlingService(repositoryProvider, gatewayProvider)
            val behandling = behandlingService.finnSisteYtelsesbehandlingFor(sak.id) ?: error("Fant ikke behandling")
            val underveisGrunnlag = UnderveisRepositoryImpl(it).hent(behandling.id)
            val tilkjentYtelse = TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id) ?: error("Fant ikke tilkjent ytelse")
            Pair(underveisGrunnlag, tilkjentYtelse)
        }

        val underveisPeriodeForUkjentDatoFørEndring = underveisGrunnlagFørEndring.perioder.find { it.periode.inneholder(justertDato) }
        val underveisPeriodeForSøknadsdatoFørEndring = underveisGrunnlagFørEndring.perioder.find { it.periode.inneholder(søknadsdato) } ?: error("Fant ikke underveisperiode for søknadstidspunkt")
        assertThat(underveisPeriodeForUkjentDatoFørEndring).isNull()
        assertThat(underveisPeriodeForSøknadsdatoFørEndring.arbeidsgradering.andelArbeid).isEqualTo(`0_PROSENT`)
        assertThat(underveisPeriodeForSøknadsdatoFørEndring.arbeidsgradering.gradering).isEqualTo(`100_PROSENT`)

        val tilkjentPeriodeForUkjentDatoFørEndring = tilkjentYtelseFørEndring.find { it.periode.inneholder(justertDato) }
        val tilkjentPeriodeForSøknadsdatoFørEndring = tilkjentYtelseFørEndring.find { it.periode.inneholder(søknadsdato) } ?: error("Fant ikke tilkjentperiode for søknadstidspunkt")
        assertThat(tilkjentPeriodeForUkjentDatoFørEndring).isNull()
        assertThat(tilkjentPeriodeForSøknadsdatoFørEndring.tilkjent.redusertDagsats().verdi).isGreaterThan(BigDecimal.ZERO)

        val avklaringsbehovManuellRevurdering =
            listOf(Vurderingsbehov.VURDER_RETTIGHETSPERIODE)
        val revurdering = sak.opprettManuellRevurdering(avklaringsbehovManuellRevurdering)
            .medKontekst {
                assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }.løsRettighetsperiode(justertDato)
            .løsSykdom(justertDato)
            .løsBistand(justertDato)
            .løsSykdomsvurderingBrev()
            .bekreftVurderinger()
            .løsBeregningstidspunkt(LocalDate.now())
            .løsOppholdskrav(justertDato)
            .løsUtenSamordning()
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_ENDRING)

        val (underveisGrunnlag, tilkjentYtelse) = dataSource.transaction {
            val repositoryProvider = postgresRepositoryRegistry.provider(it)
            val behandlingService = BehandlingService(repositoryProvider, gatewayProvider)
            val behandling = behandlingService.finnSisteYtelsesbehandlingFor(sak.id) ?: error("Fant ikke behandling")
            val underveisGrunnlag = UnderveisRepositoryImpl(it).hent(revurdering.id)
            val tilkjentYtelse = TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(revurdering.id) ?: error("Fant ikke tilkjent ytelse")
            Pair(underveisGrunnlag, tilkjentYtelse)
        }

        val underveisPeriodeForUkjentDato = underveisGrunnlag.perioder.find { it.periode.inneholder(justertDato) } ?: error("Fant ikke underveisperiode for nytt starttidspunkt")
        val underveisPeriodeForSøknadsdato = underveisGrunnlag.perioder.find { it.periode.inneholder(søknadsdato) } ?: error("Fant ikke underveisperiode for søknadstidspunkt")
        assertThat(underveisPeriodeForUkjentDato.arbeidsgradering.andelArbeid).isEqualTo(`100_PROSENT`)
        assertThat(underveisPeriodeForUkjentDato.arbeidsgradering.gradering).isEqualTo(`0_PROSENT`)
        assertThat(underveisPeriodeForSøknadsdato.arbeidsgradering.andelArbeid).isEqualTo(`0_PROSENT`)
        assertThat(underveisPeriodeForSøknadsdato.arbeidsgradering.gradering).isEqualTo(`100_PROSENT`)

        val tilkjentPeriodeForUkjentDato = tilkjentYtelse.find { it.periode.inneholder(justertDato) }?: error("Fant ikke tilkjentperiode for nytt starttidspunkt")
        val tilkjentPeriodeForSøknadsdato = tilkjentYtelse.find { it.periode.inneholder(søknadsdato) } ?: error("Fant ikke tilkjentperiode for søknadstidspunkt")
        assertThat(tilkjentPeriodeForUkjentDato.tilkjent.redusertDagsats().verdi).isEqualTo(Beløp(0).verdi)
        assertThat(tilkjentPeriodeForSøknadsdato.tilkjent.redusertDagsats().verdi).isGreaterThan(Beløp(0).verdi)

    }
}