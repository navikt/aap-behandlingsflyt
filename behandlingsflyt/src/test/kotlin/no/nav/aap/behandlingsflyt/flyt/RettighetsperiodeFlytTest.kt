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
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
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
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class RettighetsperiodeFlytTest() : AbstraktFlytOrkestratorTest(FakeUnleash::class) {

    @Test
    fun `Skal ikke kunne overstyre rettighetsperioden på en revurdering ved å innskrenke fra søknadsdato`() {
        val sak = happyCaseFørstegangsbehandling(LocalDate.now())
        val nyStartDato = sak.rettighetsperiode.fom.plusDays(7)
        val revurdering = sak.opprettManuellRevurdering(
            listOf(Vurderingsbehov.VURDER_RETTIGHETSPERIODE)
        )

        val feil = assertThrows<UgyldigForespørselException> {
            revurdering.løsRettighetsperiode(nyStartDato)
        }
        assertThat(feil.message).contains("Kan ikke endre starttidspunkt til å gjelde ETTER søknadstidspunkt")

    }

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
            .løsBistand()
            .løsSykdomsvurderingBrev()
            .løsBeregningstidspunkt(LocalDate.now())
            .løsForutgåendeMedlemskap(førsteOverstyring)
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
                assertThat(oppdatertRettighetsperiode.fom).isEqualTo(førsteOverstyring)
            }

        /**
         * Innskrenke rettighetsperioden, men ikke etter søknadsdato
         */
        val revurderingInnskrenking = sak.opprettManuellRevurdering(avklaringsbehovManuellRevurdering)
            .medKontekst {
                assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsRettighetsperiode(andreOverstyring)
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand()
            .løsSykdomsvurderingBrev()
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
    fun `Skal kunne overstyre rettighetsperioden på en revurdering - øke perioden`() {
        val sak = happyCaseFørstegangsbehandling(LocalDate.now())
        val ident = sak.person.aktivIdent()
        val nyStartDato = sak.rettighetsperiode.fom.minusDays(7)
        var revurdering = sak.opprettManuellRevurdering(
            listOf(Vurderingsbehov.VURDER_RETTIGHETSPERIODE),
        ).medKontekst {
            assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
            assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
        }

        revurdering = revurdering
            .løsRettighetsperiode(nyStartDato)
            .løsSykdom(nyStartDato)
            .løsBistand()
            .løsSykdomsvurderingBrev()
            .løsBeregningstidspunkt(nyStartDato)
            .løsForutgåendeMedlemskap(nyStartDato)
            .løsUtenSamordning()
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_ENDRING)

        val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(revurdering.id)
        assertThat(åpneAvklaringsbehov).isEmpty()

        val oppdatertSak = hentSak(ident, sak.rettighetsperiode)

        assertThat(oppdatertSak.rettighetsperiode).isNotEqualTo(sak.rettighetsperiode)
        assertThat(oppdatertSak.rettighetsperiode).isEqualTo(
            Periode(
                nyStartDato,
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
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
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
            .løsBistand()
            .løsSykdomsvurderingBrev()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap(nyStartDato)
            .løsUtenSamordning()
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_ENDRING)

        assertThat(revurdering.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Skal kunne overstyre rettighetsperioden`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(1))
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

        sak = hentSak(sak.saksnummer)

        assertThat(sak.rettighetsperiode).isNotEqualTo(periode)
        assertThat(sak.rettighetsperiode).isEqualTo(
            Periode(
                nyStartDato,
                Tid.MAKS
            )
        )
    }

    @Test
    fun `Skal kunne overstyre rettighetsperioden hos NAY`() {
        val (sak, behandling) = sendInnFørsteSøknad()

        val ident = sak.person.aktivIdent()
        val nyStartDato = sak.rettighetsperiode.fom.minusDays(7)
        behandling
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()

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
            .løsBistand()
            .løsBeregningstidspunkt(nyStartDato)
            .løsForutgåendeMedlemskap(nyStartDato)
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
    fun `Skal kunne utvide rettighetsperioden automatisk uten at vilkårene er begrenset i tidslengde`() {
        val (sak, behandling) = sendInnFørsteSøknad()
        val startDato = sak.rettighetsperiode.fom

        /**
         * Begrenser rettighetsperioden til 1 år for å simulere eksisterende behandlinger fra tidligere
         */
        val gammelRettighetsperiode = Periode(startDato, startDato.plusMonths(12).minusDays(1))
        dataSource.transaction { connection ->
            val sakRepository = SakRepositoryImpl(connection)
            sakRepository.oppdaterRettighetsperiode(
                sak.id,
                gammelRettighetsperiode
            )
        }
        behandling
            .løsSykdom(startDato)
            .løsBistand()
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt(startDato)
            .løsForutgåendeMedlemskap(startDato)
            .løsOppholdskrav(startDato)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_INNVILGELSE)

        val underveisGrunnlag: UnderveisGrunnlag =
            dataSource.transaction { UnderveisRepositoryImpl(it).hent(behandling.id) }
        val utfallTidslinje = underveisGrunnlag.perioder.somTidslinje { it.periode }.map { it.utfall }.komprimer()
        assertTidslinje(
            utfallTidslinje,
            gammelRettighetsperiode to { assertThat(it).isEqualTo(Utfall.OPPFYLT) }
        )

        val oppdatertRettighetsperiode = Periode(startDato, Tid.MAKS)
        dataSource.transaction { connection ->
            val sakRepository = SakRepositoryImpl(connection)
            sakRepository.oppdaterRettighetsperiode(
                sak.id,
                oppdatertRettighetsperiode
            )
        }

        val oppdatertBehandling =
            sak.opprettManuellRevurdering(Vurderingsbehov.REVURDER_SAMORDNING_ANDRE_FOLKETRYGDYTELSER)
        val startDatoRettTilYtelse = startDato.plusMonths(6)
        oppdatertBehandling.løsAvklaringsBehov(
            AvklarSamordningGraderingLøsning(
                VurderingerForSamordning(
                    begrunnelse = "Sykepengervurdering",
                    maksDatoEndelig = true,
                    fristNyRevurdering = null,
                    vurderteSamordningerData = listOf(
                        SamordningVurderingData(
                            ytelseType = Ytelse.SYKEPENGER,
                            periode = Periode(
                                fom = startDato,
                                tom = startDatoRettTilYtelse.minusDays(1),
                            ),
                            gradering = 100,
                            manuell = true
                        )
                    ),
                )
            )

        )
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_ENDRING)


        val sisteBehandling = hentSisteOpprettedeBehandlingForSak(sak.id)
        val oppdatertUnderveisGrunnlag: UnderveisGrunnlag =
            dataSource.transaction { UnderveisRepositoryImpl(it).hent(sisteBehandling.id) }
        val oppdatertUtfallTidslinje =
            oppdatertUnderveisGrunnlag.perioder.somTidslinje { it.periode }.map { it.utfall }.komprimer()
        assertTidslinje(
            oppdatertUtfallTidslinje,
            Periode(
                startDato,
                startDatoRettTilYtelse.minusDays(1)
            ) to { assertThat(it).isEqualTo(Utfall.IKKE_OPPFYLT) },
            Periode(
                startDatoRettTilYtelse,
                startDatoRettTilYtelse.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)
            ) to { assertThat(it).isEqualTo(Utfall.OPPFYLT) },
        )
    }

}