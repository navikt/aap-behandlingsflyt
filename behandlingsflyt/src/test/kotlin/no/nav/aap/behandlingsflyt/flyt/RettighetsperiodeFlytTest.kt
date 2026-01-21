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
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
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
import no.nav.aap.behandlingsflyt.prosessering.`OpprettBehandlingUtvidVedtakslengdeJobbUtfører`
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingService
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.motor.JobbInput
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
            .løsBistand(førsteOverstyring)
            .løsSykdomsvurderingBrev()
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
            .løsBistand(sak.rettighetsperiode.fom)
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
            .løsBistand(nyStartDato)
            .løsSykdomsvurderingBrev()
            .løsBeregningstidspunkt(nyStartDato)
            .løsOppholdskrav(nyStartDato)
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
            .løsBistand(sak.rettighetsperiode.fom)
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
            .løsBistand(nyStartDato)
            .løsSykdomsvurderingBrev()
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
    fun `Skal kunne overstyre rettighetsperioden hos NAY`() {
        val (sak, behandling) = sendInnFørsteSøknad()

        val ident = sak.person.aktivIdent()
        val nyStartDato = sak.rettighetsperiode.fom.minusDays(7)
        behandling
            .løsSykdom(sak.rettighetsperiode.fom)
            .løsBistand(sak.rettighetsperiode.fom)
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
            .løsBistand(nyStartDato)
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
    fun `Skal kunne migrere rettighetsperioden automatisk og utvide vilkårslengden`() {
        /**
         * Begrenser rettighetsperioden til 1 år for å simulere eksisterende behandlinger fra tidligere
         */
        val gammelRettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(12).minusDays(1))
        val (midlertidigSak, behandling) = sendInnFørsteSøknad()
        settRettighetsperiodeOgRekjørFraStart(midlertidigSak, gammelRettighetsperiode, behandling)
        val sak = hentSak(behandling)
        val startDato = sak.rettighetsperiode.fom

        behandling
            .løsSykdom(startDato)
            .løsBistand(startDato)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt(startDato)
            .løsOppholdskrav(startDato)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_INNVILGELSE)

        val utfallTidslinje = dataSource.transaction {
            UnderveisRepositoryImpl(it).hent(behandling.id)
        }.perioder.somTidslinje { it.periode }.map { it.utfall }.komprimer()

        assertTidslinje(
            utfallTidslinje,
            gammelRettighetsperiode to { assertThat(it).isEqualTo(Utfall.OPPFYLT) }
        )

        migrerRettighetsperiodeTilTidMaks(startDato, sak)
        opprettAtomærAutomatiskOppdaterVilkårBehandling(sak)
        val oppdatertBehandling = hentSisteOpprettedeBehandlingForSak(sak.id)

        val utfallTidslinjeOppdatert = dataSource.transaction {
            UnderveisRepositoryImpl(it).hent(oppdatertBehandling.id)
        }.perioder.somTidslinje { it.periode }.map { it.utfall }.komprimer()

        assertTidslinje(
            utfallTidslinjeOppdatert,
            Periode(startDato, startDato.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)) to {
                assertThat(it).isEqualTo(Utfall.OPPFYLT)
            }
        )
        val vilkårsresultat = dataSource.transaction { VilkårsresultatRepositoryImpl(it).hent(oppdatertBehandling.id) }
        vilkårsresultat.alle()
            .filterNot { it.type in vilkårSomIkkeAutomatiskUtvides() }
            .forEach { vilkår ->
                val vilkårSluttdato = vilkår.tidslinje().perioder().max().tom
                assertThat(vilkårSluttdato)
                    .withFailMessage { "til-dato for vilkår ${vilkår.type} er ikke Tid.MAKS men $vilkårSluttdato" }
                    .isEqualTo(Tid.MAKS)
            }
    }

    @Test
    fun `skal kunne migrere rettighetsperioder med samordning på starten av perioden`() {
        /**
         * Begrenser rettighetsperioden til 1 år for å simulere eksisterende behandlinger fra tidligere
         */
        val gammelRettighetsperiode = Periode(LocalDate.now(), LocalDate.now().plusMonths(12).minusDays(1))
        val (midlertidigSak, behandling) = sendInnFørsteSøknad()
        settRettighetsperiodeOgRekjørFraStart(midlertidigSak, gammelRettighetsperiode, behandling)
        val sak = hentSak(behandling)
        val startDato = sak.rettighetsperiode.fom
        val startDatoRettTilYtelse = sak.rettighetsperiode.fom.plusMonths(2)

        behandling
            .løsSykdom(startDato)
            .løsBistand(startDato)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt(startDato)
            .løsOppholdskrav(startDato)
            .løsAvklaringsBehov(medSamordningSykepenger(startDato, startDatoRettTilYtelse))
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_INNVILGELSE)

        val underveisGrunnlag: UnderveisGrunnlag =
            dataSource.transaction { UnderveisRepositoryImpl(it).hent(behandling.id) }
        val utfallTidslinje = underveisGrunnlag.perioder.somTidslinje { it.periode }.map { it.utfall }.komprimer()
        assertTidslinje(
            utfallTidslinje,
            Periode(
                startDato,
                startDatoRettTilYtelse.minusDays(1)
            ) to { assertThat(it).isEqualTo(Utfall.IKKE_OPPFYLT) },
            Periode(startDatoRettTilYtelse, gammelRettighetsperiode.tom) to { assertThat(it).isEqualTo(Utfall.OPPFYLT) }
        )

        migrerRettighetsperiodeTilTidMaks(startDato, sak)
        opprettAtomærAutomatiskOppdaterVilkårBehandling(sak)
        sak.sendInnMeldekort(
            journalpostId = journalpostId(),
            timerArbeidet = mapOf(startDato to 5.0),
        )
        val sisteBehandling = hentSisteOpprettedeBehandlingForSak(sak.id)
        val vilkårsResultat = dataSource.transaction { VilkårsresultatRepositoryImpl(it).hent(sisteBehandling.id) }
        assertThat(vilkårsResultat).isNotNull

        val oppdatertUtfallTidslinje =
            dataSource.transaction { UnderveisRepositoryImpl(it).hent(sisteBehandling.id) }
                .perioder.somTidslinje { it.periode }.map { it.utfall }.komprimer()
        assertTidslinje(
            oppdatertUtfallTidslinje,
            Periode(
                startDato,
                startDatoRettTilYtelse.minusDays(1)
            ) to { assertThat(it).isEqualTo(Utfall.IKKE_OPPFYLT) },
            Periode(
                startDatoRettTilYtelse,
                utfallTidslinje.maxDato()
            ) to { assertThat(it).isEqualTo(Utfall.OPPFYLT) },
        )
        val vilkårsresultat = dataSource.transaction { VilkårsresultatRepositoryImpl(it).hent(sisteBehandling.id) }
        vilkårsresultat.alle()
            .filterNot { it.type in vilkårSomIkkeAutomatiskUtvides() }
            .forEach { vilkår ->
                val vilkårSluttdato = vilkår.tidslinje().perioder().max().tom
                assertThat(vilkårSluttdato)
                    .withFailMessage { "til-dato for vilkår ${vilkår.type} er ikke Tid.MAKS men $vilkårSluttdato" }
                    .isEqualTo(Tid.MAKS)
            }

    }

    /**
     * TODO: Disse burde ideellt sett også blitt utvidet (minus samordning) ved
     * migrering av rettighetsperioden, men må sjekkes når periodisering er "avklart"
     */
    private fun vilkårSomIkkeAutomatiskUtvides() = listOf(
        Vilkårtype.OVERGANGUFØREVILKÅRET,
        Vilkårtype.OVERGANGARBEIDVILKÅRET,
        Vilkårtype.SYKEPENGEERSTATNING,
        Vilkårtype.SAMORDNING
    )


    /**
     * Denne trengs for å simulere gamle behandlinger i Kelvin som hadde 1 år med rettighetsperiode fra start
     */
    private fun settRettighetsperiodeOgRekjørFraStart(
        midlertidigSak: Sak,
        rettighetsperiode: Periode,
        behandling: Behandling
    ) {
        settRettighetsperiode(midlertidigSak, rettighetsperiode)

        // Må nullstille vilkår og rekjøre fra start
        dataSource.transaction { connection ->
            val vilkårsresultat = Vilkårsresultat()
            Vilkårtype
                .entries
                .filter { it.obligatorisk }
                .forEach { vilkårstype ->
                    vilkårsresultat
                        .leggTilHvisIkkeEksisterer(vilkårstype)
                        .leggTilIkkeVurdertPeriode(rettighetsperiode)
                }

            VilkårsresultatRepositoryImpl(connection).lagre(behandling.id, vilkårsresultat)
            Driftfunksjoner(postgresRepositoryRegistry.provider(connection), gatewayProvider).kjørFraSteg(
                behandling,
                StegType.VURDER_LOVVALG
            )
        }
    }

    private fun migrerRettighetsperiodeTilTidMaks(startDato: LocalDate, sak: Sak) {
        val oppdatertRettighetsperiode = Periode(startDato, Tid.MAKS)
        settRettighetsperiode(sak, oppdatertRettighetsperiode)
    }

    private fun medSamordningSykepenger(
        startDato: LocalDate,
        startDatoRettTilYtelse: LocalDate
    ): AvklarSamordningGraderingLøsning = AvklarSamordningGraderingLøsning(
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

    private fun opprettAtomærAutomatiskOppdaterVilkårBehandling(sak: Sak) {
        dataSource.transaction { connection ->
            val repositoryProvider = postgresRepositoryRegistry.provider(connection)
            val sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider)
            val prosesserBehandlingService = ProsesserBehandlingService(repositoryProvider, gatewayProvider)
            val behandling = sakOgBehandlingService.finnEllerOpprettBehandling(
                sakId = sak.id,
                vurderingsbehovOgÅrsak = VurderingsbehovOgÅrsak(
                    årsak = ÅrsakTilOpprettelse.MIGRER_RETTIGHETSPERIODE,
                    vurderingsbehov = listOf(VurderingsbehovMedPeriode(type = no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov.MIGRER_RETTIGHETSPERIODE))
                ),
            )
            prosesserBehandlingService.triggProsesserBehandling(behandling)
        }
    }
}