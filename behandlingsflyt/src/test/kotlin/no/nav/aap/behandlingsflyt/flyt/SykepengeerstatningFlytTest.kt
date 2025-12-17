package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarOvergangUføreLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plusHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plussEtÅrMedHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.ÅrMedHverdager
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.flate.OvergangUføreLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerGrunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykdomsvurderingLøsningDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykepengerVurderingDto
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SykepengeerstatningFlytTest : AbstraktFlytOrkestratorTest(FakeUnleash::class) {

    @Test
    fun `Sykepengeerstatning med yrkesskade`() {
        val fom = 1 april 2025
        val person = TestPersoner.PERSON_MED_YRKESSKADE()
        val periode = Periode(fom, fom.plusYears(3))
        var (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            mottattTidspunkt = fom.atStartOfDay(),
            periode = periode,
        )

        behandling = behandling.medKontekst {
            assertThat(åpneAvklaringsbehov).isNotEmpty()
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }
            .løsSykdom(
                sak.rettighetsperiode.fom,
                vissVarighet = false,
                erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).describedAs(
                    "Bistandsbehov skal ikke vurderes hvis viss varighet er nei"
                ).doesNotContain(Definisjon.AVKLAR_BISTANDSBEHOV)
            }
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).describedAs(
                    "Person med yrkesskade skal gi avklaringsbehov for yrkesskade"
                ).containsExactly(Definisjon.AVKLAR_YRKESSKADE)
            }
            .løsYrkesskade(person)
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).describedAs(
                    "Viss varighet false skal gi avklaringsbehov for sykepengeerstatning"
                ).containsExactly(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            }
            .løsAvklaringsBehov(
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "...",
                        dokumenterBruktIVurdering = emptyList(),
                        harRettPå = true,
                        grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR,
                        gjelderFra = periode.fom
                    ),
                )
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).describedAs(
                    "Forutgående medlemskap skal ikke vurderes for yrkesskade"
                ).doesNotContain(Definisjon.AVKLAR_FORUTGÅENDE_MEDLEMSKAP)
            }
            .løsBeregningstidspunkt(periode.fom)
            .løsYrkesskadeInntekt(person.yrkesskade)
            .løsOppholdskrav(periode.fom)
            .løsAndreStatligeYtelser()
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .assertRettighetstype(
                Periode(
                    sak.rettighetsperiode.fom,
                    sak.rettighetsperiode.fom.plusHverdager(Hverdager(130)).minusDays(1),
                ) to RettighetsType.SYKEPENGEERSTATNING,
            )
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)

                val resultat = dataSource.transaction {
                    ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id)
                }
                assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)
            }

    }


    @Test
    fun `avslag på 11-6 og 11-18 er også inngang til 11-13 og 11-18`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        val nå = LocalDate.now()
        // Sender inn en søknad
        var (_, behandling) = sendInnFørsteSøknad(periode = periode)
        behandling
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    løsningerForPerioder = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123128")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = true,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            fom = periode.fom,
                            tom = null
                        )
                    )
                ),
            )
            // Nei på 11-6
            .løsBistand(periode.fom, false)
            .løsAvklaringsBehov(
                AvklarOvergangUføreLøsning(
                    listOf(
                        OvergangUføreLøsningDto(
                            begrunnelse = "Løsning",
                            brukerHarSøktOmUføretrygd = true,
                            brukerHarFåttVedtakOmUføretrygd = "NEI",
                            brukerRettPåAAP = true,
                            fom = nå,
                            tom = null,
                            overgangBegrunnelse = null
                        )
                    )
                )
            )
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.AVKLAR_SYKEPENGEERSTATNING) }
                val avklaringsbehov =
                    åpneAvklaringsbehov.first { it.definisjon == Definisjon.AVKLAR_SYKEPENGEERSTATNING }
                assertThat(
                    avklaringsbehov.perioderVedtaketBehøverVurdering()
                        ?.map { it.fom }).describedAs("SPE må vurderes etter varighetsslutt for overganguføre")
                    .containsExactly(periode.fom.plusMonths(8))
            }
            .løsAvklaringsBehov(
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "...",
                        dokumenterBruktIVurdering = emptyList(),
                        harRettPå = true,
                        grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR,
                        gjelderFra = periode.fom.plusMonths(8)
                    ),
                )
            )
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap(periode.fom)
            .løsOppholdskrav(periode.fom)
            .løsAndreStatligeYtelser()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { avklaringsbehov -> assertThat(avklaringsbehov.definisjon == Definisjon.FORESLÅ_VEDTAK).isTrue() }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            // Saken står til en-trinnskontroll hos saksbehandler klar for å bli sendt til beslutter
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtak()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)
            }

        var resultat =
            dataSource.transaction { ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id) }
        assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)


        behandling = behandling.løsVedtaksbrev(typeBrev = TypeBrev.VEDTAK_11_18)

        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
        val sykepeengeerstatningsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKEPENGEERSTATNING)

        assertTidslinje(
            sykepeengeerstatningsvilkåret.tidslinje(),
            Periode(periode.fom, periode.fom.plusMonths(8).minusDays(1)) to {
                assertThat(it.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
            },
            Periode(periode.fom.plusMonths(8), Tid.MAKS) to {
                assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT)
            })

        resultat =
            dataSource.transaction { ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id) }

        assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)
        val underveisPeriode = dataSource.transaction {
            UnderveisRepositoryImpl(it).hent(behandling.id)
        }.somTidslinje().helePerioden()

        assertTidslinje(
            vilkårsresultat.rettighetstypeTidslinje().begrensetTil(underveisPeriode),
            Periode(periode.fom, periode.fom.plusMonths(8).minusDays(1)) to {
                assertThat(it).isEqualTo(RettighetsType.VURDERES_FOR_UFØRETRYGD)
            },
            Periode(periode.fom.plusMonths(8), underveisPeriode.tom) to {
                assertThat(it).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
            }
        )
    }

    @Test
    fun `revurdere sykepengeerstatning - skal ikke trigge 11-13 om gjelderFra ikke er kravdato`() {
        val sak = happyCaseFørstegangsbehandling()
        val gjelderFra = sak.rettighetsperiode.fom.plusMonths(1)

        revurdereFramTilOgMedSykdom(sak, gjelderFra)
            .løsBistand(gjelderFra)
            .løsSykdomsvurderingBrev()
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .describedAs("Siden vurderingenGjelderFra ikke er lik kravdato (rettighetsperiode.fom), så skal man ikke vurdere 11-13")
                    .containsExactlyInAnyOrder(Definisjon.FATTE_VEDTAK)  // ingen avklaringsbehov løst av NAY, gå rett til fatte vedtak
            }
            .fattVedtak()
            .medKontekst {
                val underveisGrunnlag = dataSource.transaction { connection ->
                    UnderveisRepositoryImpl(connection).hent(this.behandling.id)
                }

                assertThat(underveisGrunnlag.perioder).isNotEmpty
                assertThat(underveisGrunnlag.perioder).extracting<RettighetsType>(Underveisperiode::rettighetsType)
                    .allSatisfy { rettighetsType ->
                        assertThat(rettighetsType).isEqualTo(RettighetsType.BISTANDSBEHOV)
                    }
            }
    }

    @Test
    fun `ikke sykdom viss varighet, men skal få innvilget 11-13 sykepengererstatning`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))

        // Sender inn en søknad
        var (sak, behandling) = sendInnFørsteSøknad(periode = periode, mottattTidspunkt = periode.fom.atStartOfDay())

        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        behandling = behandling.løsAvklaringsBehov(
            AvklarSykdomLøsning(
                løsningerForPerioder = listOf(
                    SykdomsvurderingLøsningDto(
                        begrunnelse = "Er syk nok",
                        dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                        harSkadeSykdomEllerLyte = true,
                        erSkadeSykdomEllerLyteVesentligdel = true,
                        erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                        // Nei på denne gir mulighet til å innvilge på 11-13
                        erNedsettelseIArbeidsevneAvEnVissVarighet = false,
                        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                        erArbeidsevnenNedsatt = true,
                        yrkesskadeBegrunnelse = null,
                        fom = sak.rettighetsperiode.fom,
                        tom = null
                    )
                )
            ),
        )
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .contains(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            }
            .løsAvklaringsBehov(
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "...",
                        dokumenterBruktIVurdering = emptyList(),
                        harRettPå = true,
                        grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR,
                        gjelderFra = sak.rettighetsperiode.fom
                    ),
                )
            )
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap(periode.fom)
            .løsOppholdskrav(periode.fom)
            .løsAndreStatligeYtelser()
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { avklaringsbehov ->
                    assertThat(avklaringsbehov.definisjon).isEqualTo(
                        Definisjon.FORESLÅ_VEDTAK
                    )
                }
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning()).medKontekst {
                // Saken står til To-trinnskontroll hos beslutter
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon == Definisjon.FATTE_VEDTAK).isTrue() }
                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtak()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.IVERKSETTES)

                val resultat = dataSource.transaction {
                    ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(behandling.id)
                }
                assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)
            }
            .løsVedtaksbrev()
            .medKontekst {
                assertThat(this.behandling.status()).isEqualTo(Status.AVSLUTTET)

                val vilkårsresultat = hentVilkårsresultat(behandlingId = behandling.id)
                val sykepengeerstatningvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKEPENGEERSTATNING)

                assertThat(sykepengeerstatningvilkåret.vilkårsperioder()).hasSize(1).first()
                    .extracting(Vilkårsperiode::erOppfylt, Vilkårsperiode::innvilgelsesårsak)
                    .containsExactly(true, null)

                val resultat =
                    dataSource.transaction {
                        ResultatUtleder(postgresRepositoryRegistry.provider(it)).utledResultat(
                            behandling.id
                        )
                    }
                val underveisGrunnlag = dataSource.transaction { connection ->
                    UnderveisRepositoryImpl(connection).hent(behandling.id)
                }

                assertThat(resultat).isEqualTo(Resultat.INNVILGELSE)

                assertTidslinje(
                    vilkårsresultat.rettighetstypeTidslinje()
                        .begrensetTil(underveisGrunnlag.somTidslinje().helePerioden()),
                    Periode(periode.fom, periode.fom.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR)) to {
                        assertThat(it).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                    })
            }

        // Verifisere at det går an å kun 1 mnd med sykepengeerstatning
        val revurderingFom = LocalDate.now().plusMonths(1)
        val revurdering = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
        )
            .løsSykdom(vurderingGjelderFra = revurderingFom)
            .løsBistand(revurderingFom)
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov.map { it.definisjon }).containsOnly(Definisjon.SKRIV_SYKDOMSVURDERING_BREV)
            }
            .løsSykdomsvurderingBrev()
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov.map { it.definisjon }).containsOnly(Definisjon.FATTE_VEDTAK)

                val underveisTidslinje = dataSource.transaction {
                    UnderveisRepositoryImpl(it).hent(this.behandling.id).perioder
                }.map { Segment(it.periode, it) }.let(::Tidslinje)

                val oppfyltPeriode = underveisTidslinje.filter { it.verdi.rettighetsType != null }.helePerioden()
                val vilkårsresultat = hentVilkårsresultat(behandlingId = this.behandling.id)
                val rettighetstypeTidslinje = vilkårsresultat.rettighetstypeTidslinje()

                assertThat(oppfyltPeriode.fom).isEqualTo(periode.fom)
                // Oppfylt ut rettighetsperioden
                assertThat(oppfyltPeriode.tom).isEqualTo(periode.fom.plussEtÅrMedHverdager(ÅrMedHverdager.FØRSTE_ÅR))
                assertThat(underveisTidslinje.helePerioden().fom).isEqualTo(rettighetstypeTidslinje.helePerioden().fom)

                assertTidslinje(
                    rettighetstypeTidslinje.begrensetTil(underveisTidslinje.helePerioden()),
                    Periode(periode.fom, periode.fom.plusMonths(1).minusDays(1)) to {
                        assertThat(it).isEqualTo(RettighetsType.SYKEPENGEERSTATNING)
                    },
                    Periode(periode.fom.plusMonths(1), oppfyltPeriode.tom) to {
                        assertThat(it).isEqualTo(RettighetsType.BISTANDSBEHOV)
                    }
                )
            }
            .fattVedtak()
            .løsVedtaksbrev(TypeBrev.VEDTAK_ENDRING)

        // Revurdering nr 2, innvilger sp-erstatning på nytt
        val førstePeriodeSykepengeerstatning = Periode(periode.fom, periode.fom.plusMonths(1).minusDays(1))

        val revurdering2Fom = LocalDate.now().plusMonths(2)
        val revurdering2 = sak.opprettManuellRevurdering(
            listOf(no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND),
        )
            .medKontekst {
                assertThat(this.behandling.id).isNotEqualTo(revurdering.id)
            }
            .løsAvklaringsBehov(
                AvklarSykdomLøsning(
                    løsningerForPerioder = listOf(
                        SykdomsvurderingLøsningDto(
                            begrunnelse = "Er syk nok",
                            dokumenterBruktIVurdering = listOf(JournalpostId("123123")),
                            harSkadeSykdomEllerLyte = true,
                            erSkadeSykdomEllerLyteVesentligdel = true,
                            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                            erNedsettelseIArbeidsevneAvEnVissVarighet = null,
                            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
                            erArbeidsevnenNedsatt = true,
                            yrkesskadeBegrunnelse = null,
                            fom = revurdering2Fom,
                            tom = null
                        )
                    )
                )
            )
            // Nei på 11-6
            .løsBistand(revurdering2Fom, false)
            .løsOvergangUføre()
            .apply {
                if (gatewayProvider.provide<UnleashGateway>().isEnabled(BehandlingsflytFeature.OvergangArbeid)) {
                    løsOvergangArbeid(Utfall.IKKE_OPPFYLT, periode.fom)
                }
            }
            .løsSykdomsvurderingBrev()
            .løsAvklaringsBehov(
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "...",
                        dokumenterBruktIVurdering = emptyList(),
                        harRettPå = true,
                        grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR,
                        gjelderFra = LocalDate.now().plusMonths(2)
                    ),
                )
            )
            .assertRettighetstype(
                førstePeriodeSykepengeerstatning to
                        RettighetsType.SYKEPENGEERSTATNING,
                Periode(periode.fom.plusMonths(1), periode.fom.plusMonths(2).minusDays(1)) to
                        RettighetsType.BISTANDSBEHOV,
                Periode(
                    periode.fom.plusMonths(2),
                    periode.fom.plusMonths(2).plusHverdager(
                        Hverdager(130) - førstePeriodeSykepengeerstatning.antallHverdager()
                    ).minusDays(1)
                ) to
                        RettighetsType.SYKEPENGEERSTATNING,
            )
    }

    @Test
    fun `happy case førstegangsbehandling + revurder førstegangssøknad, gi sykepengererstatning hele perioden`() {
        val sak = happyCaseFørstegangsbehandling()

        revurdereFramTilOgMedSykdom(sak, sak.rettighetsperiode.fom, vissVarighet = false)
            .løsSykdomsvurderingBrev()
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .describedAs("Siden vurderingenGjelderFra er lik kravdato (rettighetsperiode.fom), så kan man revurdere 11-13")
                    .containsExactlyInAnyOrder(Definisjon.AVKLAR_SYKEPENGEERSTATNING)
            }
            .løsAvklaringsBehov(
                AvklarSykepengerErstatningLøsning(
                    sykepengeerstatningVurdering = SykepengerVurderingDto(
                        begrunnelse = "...",
                        dokumenterBruktIVurdering = emptyList(),
                        harRettPå = true,
                        grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR,
                        gjelderFra = sak.rettighetsperiode.fom
                    ),
                )
            )
            .medKontekst {
                assertThat(this.åpneAvklaringsbehov).extracting<Definisjon> { it.definisjon }
                    .containsExactlyInAnyOrder(Definisjon.FORESLÅ_VEDTAK)
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .medKontekst {
                val underveisGrunnlag = dataSource.transaction { connection ->
                    UnderveisRepositoryImpl(connection).hent(this.behandling.id)
                }
            }
            .assertRettighetstype(
                Periode(
                    sak.rettighetsperiode.fom,
                    sak.rettighetsperiode.fom.plusHverdager(Hverdager(130)).minusDays(1)
                ) to
                        RettighetsType.SYKEPENGEERSTATNING
            )
    }


}