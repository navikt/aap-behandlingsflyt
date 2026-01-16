package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBarnetilleggLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.RegisterBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Relasjon
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvarDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingerForBarnetillegg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarnDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurderingDto
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManueltOppgittBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppgitteBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.prosessering.OpprettJobbForTriggBarnetilleggSatsJobbUtfører
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.barn.BarnRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.pip.PipRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.PersonNavn
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertTrue

class BarnFlytTest : AbstraktFlytOrkestratorTest(FakeUnleash::class) {
    val periode = Periode(LocalDate.now(), Tid.MAKS)

    @Test
    fun `barnetillegg gis fram til 18 år`() {
        val fom = LocalDate.of(2025, 12, 15).minusMonths(3)
        val periode = Periode(fom, Tid.MAKS)

        val barnfødseldato = fom.minusYears(17).minusMonths(2)

        val barnIdent = genererIdent(barnfødseldato)
        val barnNavn = PersonNavn("Lille", "Larsson")
        val person = TestPersoner.STANDARD_PERSON().medBarn(
            listOf(
                TestPerson(
                    identer = setOf(barnIdent),
                    navn = barnNavn,
                    fødselsdato = Fødselsdato(barnfødseldato),
                ),
            )
        )

        val (sak, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )
        behandling
            .løsSykdom(periode.fom)
            .løsBistand(periode.fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsAndreStatligeYtelser()
            .løsBarnetillegg()

        val barn = dataSource.transaction {
            BarnRepositoryImpl(it).hent(behandling.id)
        }

        // Verifiser at barn faktisk blir fanget opp
        assertThat(barn)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes("[a-zA-Z]+\\.id")
            .ignoringCollectionOrder()
            .isEqualTo(
                BarnGrunnlag(
                    registerbarn = RegisterBarn(
                        id = -1,
                        barn = listOf(
                            Barn(
                                BarnIdentifikator.BarnIdent(
                                    ident = barnIdent,
                                    navn = barnNavn.fornavn + " " + barnNavn.etternavn,
                                    fødselsdato = Fødselsdato(barnfødseldato)
                                ),
                                navn = barnNavn.fornavn + " " + barnNavn.etternavn,
                                fødselsdato = Fødselsdato(barnfødseldato)
                            )
                        )
                    ),
                    oppgitteBarn = null,
                    vurderteBarn = null,
                    saksbehandlerOppgitteBarn = null
                )
            )

        val uthentetTilkjentYtelse =
            requireNotNull(dataSource.transaction { TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id) })
            { "Tilkjent ytelse skal være beregnet her." }

        val barnetillegg = uthentetTilkjentYtelse.map { Segment(it.periode, it.tilkjent.barnetillegg) }.let(::Tidslinje)

        val barnBlirAttenPå = barnfødseldato.plusYears(18)

        val periodeBarnUnderAtten = Periode(periode.fom, barnBlirAttenPå.minusDays(1))
        val barnErAtten = barnetillegg.begrensetTil(periodeBarnUnderAtten)

        assertThat(barnErAtten.segmenter()).isNotEmpty
        // Verifiser at barnetillegg kun gis fram til barnet er 18 år
        assertTidslinje(
            barnErAtten,
            Periode(fom = periodeBarnUnderAtten.fom, tom = LocalDate.of(2025, 12, 31)) to {
                assertThat(it).isEqualTo(Beløp(37))
            },
            Periode(fom = LocalDate.of(2026, 1, 1), tom = periodeBarnUnderAtten.tom) to {
                assertThat(it).isEqualTo(Beløp(38))
            }
        )

        val periodeBarnOverAtten = Periode(barnBlirAttenPå, uthentetTilkjentYtelse.maxOf { it.periode.tom })
        val barnErOverAtten = barnetillegg.begrensetTil(periodeBarnOverAtten)
        assertThat(barnErOverAtten.segmenter()).isNotEmpty
        // Verifiser at barnetillegg er null etter fylte 18 år
        assertTidslinje(barnErOverAtten, periodeBarnOverAtten to {
            assertThat(it).isEqualTo(Beløp(0))
        })

        // Test å fange opp sak med barnetillegg
        behandling
            .løsForeslåVedtak()
            .fattVedtak()

        val sakerMedBarnetillegg = dataSource.transaction {
            SakRepositoryImpl(it).finnSakerMedBarnetillegg(LocalDate.of(2026, 1, 1))
        }
        assertThat(sakerMedBarnetillegg).containsExactly(behandling.sakId)

        OpprettJobbForTriggBarnetilleggSatsJobbUtfører.jobbKonfigurasjon =
            OpprettJobbForTriggBarnetilleggSatsJobbUtfører.jobbKonfigurasjon.copy(erAktiv = true)

        // Bestiller brev om barnetillegg sats regulering
        dataSource.transaction {
            FlytJobbRepository(it).leggTil(JobbInput(OpprettJobbForTriggBarnetilleggSatsJobbUtfører))
        }
        motor.kjørJobber()
        val behandlingBarnetilleggSatsRegulering = hentSisteOpprettedeBehandlingForSak(behandling.sakId)
        assertThat(behandlingBarnetilleggSatsRegulering.id).isNotEqualTo(behandling.id)
        assertThat(behandlingBarnetilleggSatsRegulering.årsakTilOpprettelse).isEqualTo(ÅrsakTilOpprettelse.BARNETILLEGG_SATSENDRING)
        assertThat(behandlingBarnetilleggSatsRegulering.status()).isEqualTo(Status.AVSLUTTET)

        assertThat(hentBrevAvTypeForSak(sak, TypeBrev.BARNETILLEGG_SATS_REGULERING)).singleElement()
            .satisfies({
                assertThat(it.status).isEqualTo(no.nav.aap.behandlingsflyt.behandling.brev.bestilling.Status.FULLFØRT)
                assertThat(it.behandlingId).isEqualTo(behandlingBarnetilleggSatsRegulering.id)
            })

        // Bestiller ikke duplikat brev om barnetillegg sats regulering
        dataSource.transaction {
            FlytJobbRepository(it).leggTil(JobbInput(OpprettJobbForTriggBarnetilleggSatsJobbUtfører))
        }
        motor.kjørJobber()

        // ikke flere etter siste behandling
        assertThat(hentBrevAvTypeForSak(sak, TypeBrev.BARNETILLEGG_SATS_REGULERING)).hasSize(1)
    }

    @Test
    fun `barnetillegg gis ikke for gamle barn`() {
        val fom = LocalDate.of(2025, 12, 4)
        val periode = Periode(fom, Tid.MAKS)

        val ungtBarnFødselsdato = fom.minusYears(7)
        val gammeltBarnFødselsdato = fom.minusYears(20)

        val person = TestPersoner.STANDARD_PERSON().medBarn(
            listOf(
                TestPerson(
                    identer = setOf(Ident("aaa")),
                    navn = PersonNavn("Lille", "Larsson"),
                    fødselsdato = Fødselsdato(ungtBarnFødselsdato),
                ),
                TestPerson(
                    identer = setOf(Ident("ccc")),
                    navn = PersonNavn("Gammel", "Gustavsson"),
                    fødselsdato = Fødselsdato(gammeltBarnFødselsdato),
                ),
            )
        )

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
        )
        behandling
            .løsSykdom(periode.fom)
            .løsBistand(periode.fom)
            .løsRefusjonskrav(periode.fom)
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .løsAndreStatligeYtelser()
            .løsBarnetillegg()

        val barn = dataSource.transaction {
            BarnRepositoryImpl(it).hent(behandling.id)
        }

        // Verifiser at barn faktisk blir fanget opp
        assertThat(barn)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .ignoringFieldsMatchingRegexes("[a-zA-Z]+\\.id").isEqualTo(
                BarnGrunnlag(
                    registerbarn = RegisterBarn(
                        id = -1,
                        barn = person.barn.map {
                            Barn(
                                BarnIdentifikator.BarnIdent(
                                    ident = it.aktivIdent(),
                                    navn = "${it.navn.fornavn} ${it.navn.etternavn}",
                                    fødselsdato = it.fødselsdato
                                ),
                                fødselsdato = it.fødselsdato,
                                navn = "${it.navn.fornavn} ${it.navn.etternavn}"
                            )
                        }),
                    oppgitteBarn = null,
                    vurderteBarn = null,
                    saksbehandlerOppgitteBarn = null
                )
            )

        val uthentetTilkjentYtelse =
            requireNotNull(dataSource.transaction { TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id) })
            { "Tilkjent ytelse skal være beregnet her." }

        val barnetillegg = uthentetTilkjentYtelse.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)
        val tilkjentYtelsePeriode = uthentetTilkjentYtelse.tilTidslinje().helePerioden()

        assertThat(barnetillegg.segmenter()).isNotEmpty

        // Skal kun gi barnetillegg for det unge barnet
        assertTidslinje(
            barnetillegg,
            Periode(fom = periode.fom, tom = LocalDate.of(2025, 12, 31)) to {
                assertThat(it.barnetillegg).isEqualTo(Beløp(37))
                assertThat(it.antallBarn).isEqualTo(1)
            },
            Periode(fom = LocalDate.of(2026, 1, 1), tom = tilkjentYtelsePeriode.tom) to {
                assertThat(it.barnetillegg).isEqualTo(Beløp(38))
                assertThat(it.antallBarn).isEqualTo(1)
            })
    }

    @Test
    fun `oppgir manuelle barn, avklarer dem`() {
        val fom = LocalDate.now()
        val periode = Periode(fom, fom.plusYears(3))

        val person = TestPersoner.STANDARD_PERSON()

        val manueltBarnIPDL = TestPerson(
            navn = PersonNavn("Yousef", "Yosso"),
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(12))
        )
        // Dette gjør at flyten finner barnet i PDL (via FakeServers)
        FakePersoner.leggTil(manueltBarnIPDL)

        val barnNavn = "Gregor Gorgh"
        val barnAlder = LocalDate.now().minusYears(17)
        val søknad = TestSøknader.SØKNAD_MED_BARN(
            listOf(
                Pair(
                    manueltBarnIPDL.navn.toString(),
                    manueltBarnIPDL.fødselsdato.toLocalDate()
                ),
                Pair(
                    barnNavn,
                    barnAlder
                ),
            )
        )

        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
            søknad = søknad,
        )
        behandling
            .løsSykdom(periode.fom)
            .løsBistand(periode.fom)
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsOppholdskrav(fom)
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).containsExactly(Definisjon.AVKLAR_BARNETILLEGG)
            }
            .løsAvklaringsBehov(
                AvklarBarnetilleggLøsning(
                    vurderingerForBarnetillegg = VurderingerForBarnetillegg(
                        vurderteBarn = listOf(
                            VurdertBarnDto(
                                ident = null,
                                navn = barnNavn,
                                fødselsdato = barnAlder,
                                vurderinger = listOf(
                                    VurderingAvForeldreAnsvarDto(
                                        fraDato = periode.fom,
                                        harForeldreAnsvar = true,
                                        begrunnelse = "bra forelder"
                                    )
                                )
                            )
                        ),
                        emptyList()
                    ),
                ),
            )
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon })
                    .describedAs("Vi avklarte bare ett barn, behovet skal fortsatt være åpent")
                    .containsExactly(Definisjon.AVKLAR_BARNETILLEGG)

            }
            .løsAvklaringsBehov(
                AvklarBarnetilleggLøsning(
                    vurderingerForBarnetillegg = VurderingerForBarnetillegg(
                        vurderteBarn = listOf(
                            VurdertBarnDto(
                                ident = null,
                                navn = barnNavn,
                                fødselsdato = barnAlder,
                                vurderinger = listOf(
                                    VurderingAvForeldreAnsvarDto(
                                        fraDato = periode.fom,
                                        harForeldreAnsvar = true,
                                        begrunnelse = "bra forelder"
                                    )
                                )
                            ),
                            VurdertBarnDto(
                                ident = null,
                                navn = manueltBarnIPDL.navn.toString(),
                                fødselsdato = manueltBarnIPDL.fødselsdato.toLocalDate(),
                                vurderinger = listOf(
                                    VurderingAvForeldreAnsvarDto(
                                        fraDato = periode.fom,
                                        harForeldreAnsvar = true,
                                        begrunnelse = "bra forelder"
                                    )
                                )
                            )
                        ),
                        emptyList()
                    ),
                ),
            )
            .løsAndreStatligeYtelser()
            .medKontekst {
                assertThat(åpneAvklaringsbehov.map { it.definisjon }).containsExactly(Definisjon.FORESLÅ_VEDTAK)

                val tilkjentYtelse =
                    repositoryProvider.provide<TilkjentYtelseRepository>().hentHvisEksisterer(behandling.id)
                        .orEmpty().map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                val periodeMedBarneTilleggForToBarn =
                    tilkjentYtelse.filter { it.verdi.barnetillegg.verdi.toDouble() > 70 }.helePerioden()
                val periodeMedBarneTilleggForEttBarn =
                    tilkjentYtelse.filter { it.verdi.barnetillegg.verdi.toDouble() < 40 }.helePerioden()

                // Barnet er 18 fram til periode.fom.plusYears(1).minusDays(1)
                assertThat(periodeMedBarneTilleggForToBarn).isEqualTo(
                    Periode(
                        periode.fom,
                        periode.fom.plusYears(1).minusDays(1)
                    )
                )
                val underveisPeriode =
                    repositoryProvider.provide<UnderveisRepository>().hent(behandling.id).somTidslinje().helePerioden()
                assertThat(periodeMedBarneTilleggForEttBarn).isEqualTo(
                    Periode(
                        periode.fom.plusYears(1),
                        underveisPeriode.tom
                    )
                )

                assertTidslinje(
                    tilkjentYtelse,
                    periodeMedBarneTilleggForToBarn to {
                        assertThat(it.antallBarn).isEqualTo(2)
                    },
                    periodeMedBarneTilleggForEttBarn to {
                        assertThat(it.antallBarn).isEqualTo(1)
                    })
            }
    }

    @Test
    fun `barn lagres i pip i starten av behandlingen`() {
        val periode = Periode(LocalDate.now(), LocalDate.now().plusYears(3))
        val manueltBarnIdent = genererIdent(LocalDate.now().minusYears(3))
        val person = TestPersoner.STANDARD_PERSON()

        // Oppretter søknad med manuelt barn
        val (_, behandling) = sendInnFørsteSøknad(
            person = person,
            periode = periode,
            søknad = SøknadV0(
                student = SøknadStudentDto(StudentStatus.Nei), yrkesskade = "NEI", oppgitteBarn = OppgitteBarn(
                    barn = listOf(
                        ManueltOppgittBarn(
                            navn = "manuelt barn",
                            fødselsdato = LocalDate.now().minusYears(3),
                            ident = no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Ident(manueltBarnIdent.identifikator),
                            relasjon = ManueltOppgittBarn.Relasjon.FORELDER
                        )
                    ),
                    identer = emptySet()
                ),
                medlemskap = SøknadMedlemskapDto("JA", null, "NEI", null, null)
            )
        )

        dataSource.transaction { connection ->
            val pipRepository = PipRepositoryImpl(connection)
            val pipIdenter = pipRepository.finnIdenterPåBehandling(behandling.referanse)

            // Manuelt barn finnes i pip umiddelbart etter at søknad er innsendt
            assertThat(pipIdenter.map { it.ident }).containsExactlyInAnyOrder(
                person.aktivIdent().identifikator,
                manueltBarnIdent.identifikator,
            )
        }
    }

    @Test
    fun `førstegangsbehandling med barnetillegg for oppgitte, registrerte og saksbehandler-oppgitte barn`() {
        val periode = Periode(LocalDate.of(2026, 1, 1), Tid.MAKS)
        val oppgitteBarn = lagTestPerson("Tone", "Dovendyr", LocalDate.now().minusYears(9))
        val fraDatoOppgitteBarn = periode.fom
        val fraDatoRegisterBarn = LocalDate.now().plusMonths(1).plusDays(12)
        val fraDatoSaksbehandlerOppgitteBarn = LocalDate.now().plusMonths(2).plusDays(5)

        val søknadMedOppgittBarn = TestSøknader.SØKNAD_MED_BARN(
            listOf(
                Pair(
                    oppgitteBarn.navn.toString(),
                    oppgitteBarn.fødselsdato.toLocalDate()
                ),
            )
        )

        val registerBarn = lagTestPerson("Jenny", "Løvland", LocalDate.now().minusYears(7))
        val registerBarnIdent = genererIdent(registerBarn.fødselsdato.toLocalDate())
        val personMedRegistrerteBarn = TestPersoner.STANDARD_PERSON().medBarn(
            listOf(
                TestPerson(
                    identer = setOf(registerBarnIdent),
                    navn = registerBarn.navn,
                    fødselsdato = registerBarn.fødselsdato,
                )
            )
        )
        val saksbehandlerOppgitteBarn = lagTestPerson("Fornuftig", "Bukk", LocalDate.now().minusYears(3))

        val saksbehandlerOppgitteBarnVurderteBarnDto = VurdertBarnDto(
            ident = saksbehandlerOppgitteBarn.identer.first().identifikator,
            navn = saksbehandlerOppgitteBarn.navn.toString(),
            fødselsdato = saksbehandlerOppgitteBarn.fødselsdato.toLocalDate(),
            vurderinger = listOf(
                VurderingAvForeldreAnsvarDto(
                    harForeldreAnsvar = true,
                    begrunnelse = "Fint forelder",
                    fraDato = fraDatoSaksbehandlerOppgitteBarn,
                    erFosterForelder = false
                )
            ),
            oppgittForeldreRelasjon = Relasjon.FORELDER
        )

        vurdereFramTilBarnetillegg(søknadMedOppgittBarn, periode, personMedRegistrerteBarn).second
            // Løs barnetillegg avklaringsbehov
            .løsAvklaringsBehov(
                AvklarBarnetilleggLøsning(
                    vurderingerForBarnetillegg = VurderingerForBarnetillegg(
                        vurderteBarn = listOf(
                            // Oppgitte barn i søknaden
                            VurdertBarnDto(
                                ident = null,
                                navn = oppgitteBarn.navn.toString(),
                                fødselsdato = oppgitteBarn.fødselsdato.toLocalDate(),
                                vurderinger = listOf(
                                    VurderingAvForeldreAnsvarDto(
                                        harForeldreAnsvar = true,
                                        begrunnelse = "Bra forelder",
                                        fraDato = fraDatoOppgitteBarn,
                                        erFosterForelder = false
                                    )
                                ),
                                oppgittForeldreRelasjon = Relasjon.FORELDER
                            ),
                            // Register barn
                            VurdertBarnDto(
                                ident = registerBarnIdent.identifikator,
                                navn = registerBarn.navn.toString(),
                                fødselsdato = registerBarn.fødselsdato.toLocalDate(),
                                vurderinger = listOf(
                                    VurderingAvForeldreAnsvarDto(
                                        harForeldreAnsvar = true,
                                        begrunnelse = "Greit forelder",
                                        fraDato = fraDatoRegisterBarn,
                                        erFosterForelder = false
                                    )
                                ),
                                oppgittForeldreRelasjon = Relasjon.FORELDER
                            ),
                            saksbehandlerOppgitteBarnVurderteBarnDto
                        ),
                        saksbehandlerOppgitteBarn = listOf(saksbehandlerOppgitteBarnVurderteBarnDto)
                    )
                )
            )
            .løsUtenSamordning()
            .løsAndreStatligeYtelser()
            .medKontekst {
                val tilkjentYtelseFraRepo = hentTilkjentYtelse(behandling.id)

                val tilkjentYtelseTidslinje =
                    tilkjentYtelseFraRepo.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertThat(tilkjentYtelseTidslinje.isNotEmpty())

                val periodeEttBarn = Periode(fraDatoOppgitteBarn, fraDatoRegisterBarn.minusDays(1))
                val tilkjentYtelseEttBarn = tilkjentYtelseTidslinje.begrensetTil(periodeEttBarn)
                assertTidslinje(tilkjentYtelseEttBarn, periodeEttBarn to {
                    assertThat(it.barnetillegg).isEqualTo(Beløp(38))
                    assertThat(it.antallBarn).isEqualTo(1)
                })

                val periodeToBarn = Periode(fraDatoRegisterBarn, fraDatoSaksbehandlerOppgitteBarn.minusDays(1))
                val tilkjentYtelseToBarn = tilkjentYtelseTidslinje.begrensetTil(periodeToBarn)
                assertTidslinje(tilkjentYtelseToBarn, periodeToBarn to {
                    assertThat(it.barnetillegg).isEqualTo(Beløp(76))
                    assertThat(it.antallBarn).isEqualTo(2)
                })

                val periodeTreBarn =
                    Periode(fraDatoSaksbehandlerOppgitteBarn, tilkjentYtelseFraRepo.tilTidslinje().helePerioden().tom)
                val tilkjentYtelseTreBarn = tilkjentYtelseTidslinje.begrensetTil(periodeTreBarn)
                assertTidslinje(tilkjentYtelseTreBarn, periodeTreBarn to {
                    assertThat(it.barnetillegg).isEqualTo(Beløp(114))
                    assertThat(it.antallBarn).isEqualTo(3)
                })
            }
    }

    @Test
    fun `revurdering barnetillegg med ekstra saksbehandler-oppgitt barn øker utbetaling fra et til to barn`() {
        val periode = Periode(LocalDate.of(2026, 1, 1), Tid.MAKS)
        val oppgitteBarn = lagTestPerson("Lilly", "Løve", LocalDate.now().minusYears(6))

        // Sender inn en søknad
        val søknad = TestSøknader.SØKNAD_MED_BARN(
            listOf(
                Pair(
                    oppgitteBarn.navn.toString(),
                    oppgitteBarn.fødselsdato.toLocalDate()
                ),
            )
        )

        val sakOgBehandlingPar = vurdereFramTilBarnetillegg(søknad, periode, TestPersoner.STANDARD_PERSON())
        val behandling: Behandling = sakOgBehandlingPar.second
        // løs barnetillegg avklaringsbehov
        behandling.løsAvklaringsBehov(
            AvklarBarnetilleggLøsning(
                vurderingerForBarnetillegg = VurderingerForBarnetillegg(
                    vurderteBarn = listOf(
                        // Oppgitte barn i søknaden
                        VurdertBarnDto(
                            ident = null,
                            navn = oppgitteBarn.navn.toString(),
                            fødselsdato = oppgitteBarn.fødselsdato.toLocalDate(),
                            vurderinger = listOf(
                                VurderingAvForeldreAnsvarDto(
                                    harForeldreAnsvar = true,
                                    begrunnelse = "Bra forelder",
                                    fraDato = periode.fom,
                                    erFosterForelder = false
                                )
                            ),
                            oppgittForeldreRelasjon = Relasjon.FORELDER
                        ),
                    ),
                    saksbehandlerOppgitteBarn = emptyList(),
                )
            )
        )
            .løsAndreStatligeYtelser()
            .medKontekst {
                val tilkjentYtelseFraRepo = hentTilkjentYtelse(behandling.id)

                val barnetillegg = tilkjentYtelseFraRepo.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)
                val tilkjentYtelsePeriode = tilkjentYtelseFraRepo.tilTidslinje().helePerioden()

                assertTrue(barnetillegg.isNotEmpty())
                assertTidslinje(barnetillegg, tilkjentYtelsePeriode to {
                    assertThat(it.barnetillegg).isEqualTo(Beløp(38))
                    assertThat(it.antallBarn).isEqualTo(1)
                })
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .medKontekst {
                assertThat(åpneAvklaringsbehov).anySatisfy { assertThat(it.definisjon).isEqualTo(Definisjon.FATTE_VEDTAK) }
                assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
            }
            .fattVedtak()
            .løsVedtaksbrev()
            .medKontekst {
                val åpneAvklaringsbehov = hentÅpneAvklaringsbehov(behandling.id)
                assertThat(åpneAvklaringsbehov).isEmpty()
            }

        val sak: Sak = sakOgBehandlingPar.first
        val førstegangsbehandling = hentSisteOpprettedeBehandlingForSak(sak.id)

        val fomSaksbehandlerOppgitteBarn = LocalDate.now().plusMonths(1).plusDays(4)
        val saksbehandlerOppgitteBarn = VurdertBarnDto(
            ident = null,
            navn = "Flinke Bamse",
            fødselsdato = LocalDate.now().minusYears(6),
            vurderinger = listOf(
                VurderingAvForeldreAnsvarDto(
                    harForeldreAnsvar = true,
                    begrunnelse = "Greit forelder",
                    fraDato = fomSaksbehandlerOppgitteBarn,
                    erFosterForelder = false
                )
            ),
            oppgittForeldreRelasjon = Relasjon.FORELDER
        )

        // Oppretter revurdering
        val revurdering = sak.opprettManuellRevurdering(
            listOf(Vurderingsbehov.BARNETILLEGG)
        )

        revurdering.medKontekst {
            assertThat(this.behandling.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
            assertThat(revurdering.forrigeBehandlingId).isEqualTo(førstegangsbehandling.id)
            assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
        }
            .løsAvklaringsBehov(
                AvklarBarnetilleggLøsning(
                    vurderingerForBarnetillegg = VurderingerForBarnetillegg(
                        vurderteBarn = listOf(
                            // Oppgitte barn
                            VurdertBarnDto(
                                ident = null,
                                navn = oppgitteBarn.navn.toString(),
                                fødselsdato = oppgitteBarn.fødselsdato.toLocalDate(),
                                vurderinger = listOf(
                                    VurderingAvForeldreAnsvarDto(
                                        harForeldreAnsvar = true,
                                        begrunnelse = "Greit forelder",
                                        fraDato = periode.fom,
                                        erFosterForelder = false
                                    )
                                ),
                                oppgittForeldreRelasjon = Relasjon.FORELDER
                            ),
                            saksbehandlerOppgitteBarn,
                        ),
                        saksbehandlerOppgitteBarn = listOf(saksbehandlerOppgitteBarn)
                    )
                )
            )
            .medKontekst {
                val barn = repositoryProvider.provide<BarnRepository>().hent(revurdering.id)

                assertThat(barn.oppgitteBarn?.oppgitteBarn).hasSize(1)
                assertThat(barn.saksbehandlerOppgitteBarn?.barn).hasSize(1)
                assertThat(barn.vurderteBarn?.barn).hasSize(2)

                val tilkjentYtelseFraRepo = hentTilkjentYtelse(revurdering.id)

                val tilkjentYtelseTidslinje =
                    tilkjentYtelseFraRepo.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertTrue(tilkjentYtelseTidslinje.isNotEmpty())

                // Verifiser at barnetillegg øker når det andre barnet legges til
                val periodeEttBarn = Periode(periode.fom, fomSaksbehandlerOppgitteBarn.minusDays(1))
                val tilkjentYtelseEtBarn = tilkjentYtelseTidslinje.begrensetTil(periodeEttBarn)
                assertTidslinje(tilkjentYtelseEtBarn, periodeEttBarn to {
                    assertThat(it.barnetillegg).isEqualTo(Beløp(38))
                    assertThat(it.antallBarn).isEqualTo(1)
                })

                val periodeToBarn =
                    Periode(fomSaksbehandlerOppgitteBarn, tilkjentYtelseFraRepo.tilTidslinje().helePerioden().tom)
                val tilkjentYtelseToBarn = tilkjentYtelseTidslinje.begrensetTil(periodeToBarn)
                assertTidslinje(tilkjentYtelseToBarn, periodeToBarn to {
                    assertThat(it.barnetillegg).isEqualTo(Beløp(76))
                    assertThat(it.antallBarn).isEqualTo(2)
                })

            }

    }

    @Test
    fun `førstegangsbehandling barnetillegg, nei på 11-13 - avslag, ingen tilkjent ytelse`() {
        val periode = Periode(LocalDate.of(2026, 1, 1), Tid.MAKS)
        val oppgitteBarn = lagTestPerson("Tara", "Tiger", periode.fom.minusYears(15))
        val fraDatoOppgitteBarn = periode.fom.plusMonths(1).plusDays(5)

        val søknad = TestSøknader.SØKNAD_MED_BARN(
            listOf(
                Pair(
                    oppgitteBarn.navn.toString(),
                    oppgitteBarn.fødselsdato.toLocalDate()
                ),
            )
        )

        vurdereFramTilBarnetillegg(søknad, periode, TestPersoner.STANDARD_PERSON()).second
            // Løs barnetillegg avklaringsbehov
            .løsAvklaringsBehov(
                AvklarBarnetilleggLøsning(
                    vurderingerForBarnetillegg = VurderingerForBarnetillegg(
                        vurderteBarn = listOf(
                            // Oppgitte barn i søknaden
                            VurdertBarnDto(
                                ident = null,
                                navn = oppgitteBarn.navn.toString(),
                                fødselsdato = oppgitteBarn.fødselsdato.toLocalDate(),
                                vurderinger = listOf(
                                    VurderingAvForeldreAnsvarDto(
                                        harForeldreAnsvar = true,
                                        begrunnelse = "Bra forelder",
                                        fraDato = fraDatoOppgitteBarn,
                                        erFosterForelder = false
                                    )
                                ),
                                oppgittForeldreRelasjon = Relasjon.FORELDER
                            ),
                        ),
                        saksbehandlerOppgitteBarn = emptyList()
                    )
                )
            )
            .løsAndreStatligeYtelser()
            .medKontekst {
                val tilkjentYtelseFraRepo = hentTilkjentYtelse(behandling.id)

                val tilkjentYtelseTidslinje =
                    tilkjentYtelseFraRepo.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertTrue(tilkjentYtelseTidslinje.isNotEmpty())

                val periodeEttBarn =
                    Periode(fraDatoOppgitteBarn, tilkjentYtelseFraRepo.tilTidslinje().helePerioden().tom)
                val tilkjentYtelseEttBarn = tilkjentYtelseTidslinje.begrensetTil(periodeEttBarn)
                assertTidslinje(tilkjentYtelseEttBarn, periodeEttBarn to {
                    assertThat(it.barnetillegg).isEqualTo(Beløp(38))
                    assertThat(it.antallBarn).isEqualTo(1)
                })
            }
            // Gi avslag på 11-13
            .løsSykdom(vurderingGjelderFra = periode.fom, erOppfylt = false)
            .medKontekst {
                val tilkjentYtelseFraRepo = hentTilkjentYtelse(behandling.id)

                val tilkjentYtelseTidslinje =
                    tilkjentYtelseFraRepo.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertTrue(tilkjentYtelseTidslinje.isNotEmpty())

                val periodeEttBarn =
                    Periode(fraDatoOppgitteBarn, tilkjentYtelseFraRepo.tilTidslinje().helePerioden().tom)
                val tilkjentYtelseEttBarn = tilkjentYtelseTidslinje.begrensetTil(periodeEttBarn)
                assertTidslinje(tilkjentYtelseEttBarn, periodeEttBarn to {
                    assertThat(it.barnetillegg).isEqualTo(Beløp(0))
                    assertThat(it.barnetilleggsats).isEqualTo(Beløp(0))
                    assertThat(it.antallBarn).isEqualTo(0)
                })
            }
    }

    @Test
    fun `revurdering barnetillegg, nei på 11-13 - avslag, ingen barnetillegg i tilkjent ytelse`() {
        val periode = Periode(LocalDate.of(2026, 1, 1), Tid.MAKS)
        val barnfødseldato = periode.fom.minusYears(10)

        val barnIdent = genererIdent(barnfødseldato)
        val barnNavn = PersonNavn("Mari", "Måke")
        val person = TestPersoner.STANDARD_PERSON().medBarn(
            listOf(
                TestPerson(
                    identer = setOf(barnIdent),
                    navn = barnNavn,
                    fødselsdato = Fødselsdato(barnfødseldato),
                ),
            )
        )

        // Førstegangsbehandling med barnetillegg for register barn
        val sakOgBehandlingPar = vurdereFramTilBarnetillegg(TestSøknader.STANDARD_SØKNAD, periode, person)
        val behandling: Behandling = sakOgBehandlingPar.second
        behandling.løsBarnetillegg()
            .løsAndreStatligeYtelser()
            .medKontekst {
                val tilkjentYtelseFraRepo = hentTilkjentYtelse(behandling.id)

                val barnetillegg = tilkjentYtelseFraRepo.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)
                val tilkjentYtelsePeriode = tilkjentYtelseFraRepo.tilTidslinje().helePerioden()

                assertTrue(barnetillegg.isNotEmpty())
                assertTidslinje(barnetillegg, tilkjentYtelsePeriode to {
                    assertThat(it.barnetillegg).isEqualTo(Beløp(38))
                    assertThat(it.antallBarn).isEqualTo(1)
                })
            }
            .løsAvklaringsBehov(ForeslåVedtakLøsning())
            .fattVedtak()
            .løsVedtaksbrev()

        // Oppretter revurdering med avslag på 11-13
        val sak: Sak = sakOgBehandlingPar.first
        val førstegangsbehandling = hentSisteOpprettedeBehandlingForSak(sak.id)
        val revurdering = sak.opprettManuellRevurdering(
            listOf(Vurderingsbehov.SYKDOM_ARBEVNE_BEHOV_FOR_BISTAND)
        )
        revurdering.medKontekst {
            assertThat(revurdering.typeBehandling()).isEqualTo(TypeBehandling.Revurdering)
            assertThat(revurdering.forrigeBehandlingId).isEqualTo(førstegangsbehandling.id)
            assertThat(this.behandling.status()).isEqualTo(Status.UTREDES)
        }
            // Gi avslag på 11-13
            .løsSykdom(periode.fom, vissVarighet = false, erOppfylt = false)
            .løsSykdomsvurderingBrev()
            .fattVedtak()
            .medKontekst {
                val tilkjentYtelseFraRepo = hentTilkjentYtelse(revurdering.id)

                val tilkjentYtelseTidslinje =
                    tilkjentYtelseFraRepo.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertTrue(tilkjentYtelseTidslinje.isNotEmpty())

                val periodeEttBarn =
                    Periode(periode.fom, tilkjentYtelseFraRepo.tilTidslinje().helePerioden().tom)
                val tilkjentYtelseEttBarn = tilkjentYtelseTidslinje.begrensetTil(periodeEttBarn)
                assertTidslinje(tilkjentYtelseEttBarn, periodeEttBarn to {
                    assertThat(it.barnetillegg).isEqualTo(Beløp(0))
                    assertThat(it.barnetilleggsats).isEqualTo(Beløp(0))
                    assertThat(it.antallBarn).isEqualTo(0)
                })
            }

    }

    @Test
    fun `førstegangsbehandling der et barn fjernes reduserer barnetillegg tilsvarende`() {
        val periode = Periode(LocalDate.of(2026, 1, 1), Tid.MAKS)
        val registerBarn = lagTestPerson("Slapp", "Isbjørn", periode.fom.minusYears(7))
        val registerBarnIdent = genererIdent(registerBarn.fødselsdato.toLocalDate())
        val personMedRegistrerteBarn = TestPersoner.STANDARD_PERSON().medBarn(
            listOf(
                TestPerson(
                    identer = setOf(registerBarnIdent),
                    navn = registerBarn.navn,
                    fødselsdato = registerBarn.fødselsdato,
                )
            )
        )

        val saksbehandlerOppgitteBarn = VurdertBarnDto(
            ident = "hhhh",
            navn = "Kjær Boks",
            fødselsdato = LocalDate.now().minusYears(4),
            vurderinger = listOf(
                VurderingAvForeldreAnsvarDto(
                    harForeldreAnsvar = true,
                    begrunnelse = "Fint forelder",
                    fraDato = periode.fom,
                    erFosterForelder = false
                )
            ),
            oppgittForeldreRelasjon = Relasjon.FORELDER
        )

        vurdereFramTilBarnetillegg(TestSøknader.STANDARD_SØKNAD, periode, personMedRegistrerteBarn).second
            // Legg til saksbehandler-oppgitte barn
            .løsAvklaringsBehov(
                AvklarBarnetilleggLøsning(
                    vurderingerForBarnetillegg = VurderingerForBarnetillegg(
                        vurderteBarn = listOf(saksbehandlerOppgitteBarn),
                        saksbehandlerOppgitteBarn = listOf(saksbehandlerOppgitteBarn)
                    )
                )
            )
            .løsAndreStatligeYtelser()
            .medKontekst {
                val barn = repositoryProvider.provide<BarnRepository>().hent(behandling.id)

                assertThat(barn.oppgitteBarn).isNull()
                assertThat(barn.saksbehandlerOppgitteBarn?.barn).hasSize(1)
                assertThat(barn.registerbarn?.barn).hasSize(1)

                val tilkjentYtelseFraRepo = hentTilkjentYtelse(behandling.id)

                val tilkjentYtelseTidslinje =
                    tilkjentYtelseFraRepo.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertTrue(tilkjentYtelseTidslinje.isNotEmpty())

                val tilkjentYtelsePeriode = tilkjentYtelseFraRepo.tilTidslinje().helePerioden()
                val barnetillegg = tilkjentYtelseTidslinje.begrensetTil(tilkjentYtelsePeriode)
                assertTidslinje(barnetillegg, tilkjentYtelsePeriode to {
                    assertThat(it.barnetillegg).isEqualTo(Beløp(76))
                    assertThat(it.antallBarn).isEqualTo(2)
                })
            }
            // Gå tilbake til barnetillegg-steget og fjerne saksbehandler-oppgitte barn
            .løsBarnetillegg() // Her sender man inn tom liste for saksbehandler-oppgitte barn
            .løsAndreStatligeYtelser()
            .medKontekst {
                val barn = dataSource.transaction {
                    BarnRepositoryImpl(it).hent(behandling.id)
                }
                assertThat(barn.oppgitteBarn).isNull()
                assertThat(barn.saksbehandlerOppgitteBarn).isNull()
                assertThat(barn.registerbarn?.barn).hasSize(1)

                val tilkjentYtelseFraRepo = hentTilkjentYtelse(behandling.id)

                val tilkjentYtelseTidslinje =
                    tilkjentYtelseFraRepo.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertTrue(tilkjentYtelseTidslinje.isNotEmpty())

                val tilkjentYtelsePeriode = tilkjentYtelseFraRepo.tilTidslinje().helePerioden()
                val barnetillegg = tilkjentYtelseTidslinje.begrensetTil(tilkjentYtelsePeriode)
                assertTidslinje(barnetillegg, tilkjentYtelsePeriode to {
                    assertThat(it.barnetillegg).isEqualTo(Beløp(38))
                    assertThat(it.antallBarn).isEqualTo(1)
                })
            }
    }

    @Test
    fun `førstegangsbehandling legge til nytt barn gir det ekstra barnetillegg tilsvarende`() {
        val periode = Periode(LocalDate.of(2026, 1, 1), Tid.MAKS)
        val registerBarn = lagTestPerson("Tuva", "Trallala", periode.fom.minusYears(7))
        val registerBarnIdent = genererIdent(registerBarn.fødselsdato.toLocalDate())
        val personMedRegistrerteBarn = TestPersoner.STANDARD_PERSON().medBarn(
            listOf(
                TestPerson(
                    identer = setOf(registerBarnIdent),
                    navn = registerBarn.navn,
                    fødselsdato = registerBarn.fødselsdato,
                )
            )
        )
        val behandling =
            vurdereFramTilBarnetillegg(TestSøknader.STANDARD_SØKNAD, periode, personMedRegistrerteBarn).second
                .løsBarnetillegg()
                .løsAndreStatligeYtelser()
                .medKontekst {
                    val barn = dataSource.transaction {
                        BarnRepositoryImpl(it).hent(behandling.id)
                    }
                    assertThat(barn.oppgitteBarn).isNull()
                    assertThat(barn.saksbehandlerOppgitteBarn).isNull()
                    assertThat(barn.registerbarn?.barn).hasSize(1)

                    val tilkjentYtelseFraRepo = hentTilkjentYtelse(behandling.id)

                    val tilkjentYtelseTidslinje =
                        tilkjentYtelseFraRepo.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                    assertTrue(tilkjentYtelseTidslinje.isNotEmpty())

                    val tilkjentYtelsePeriode = tilkjentYtelseFraRepo.tilTidslinje().helePerioden()
                    val barnetillegg = tilkjentYtelseTidslinje.begrensetTil(tilkjentYtelsePeriode)
                    assertTidslinje(barnetillegg, tilkjentYtelsePeriode to {
                        assertThat(it.barnetillegg).isEqualTo(Beløp(38))
                        assertThat(it.antallBarn).isEqualTo(1)
                    })
                }

        // Gå tilbake til barnetillegg-steget og legge til et nytt barn
        val saksbehandlerOppgitteBarn = VurdertBarnDto(
            ident = "hhhh",
            navn = "Konkret Oktober",
            fødselsdato = LocalDate.now().minusYears(6),
            vurderinger = listOf(
                VurderingAvForeldreAnsvarDto(
                    harForeldreAnsvar = true,
                    begrunnelse = "Fint forelder",
                    fraDato = periode.fom,
                    erFosterForelder = false
                )
            ),
            oppgittForeldreRelasjon = Relasjon.FORELDER
        )
        behandling.løsAvklaringsBehov(
            AvklarBarnetilleggLøsning(
                vurderingerForBarnetillegg = VurderingerForBarnetillegg(
                    vurderteBarn = listOf(saksbehandlerOppgitteBarn),
                    saksbehandlerOppgitteBarn = listOf(saksbehandlerOppgitteBarn)
                )
            )
        )
            .løsAndreStatligeYtelser()
            .medKontekst {
                val barn = dataSource.transaction {
                    BarnRepositoryImpl(it).hent(behandling.id)
                }
                assertThat(barn.oppgitteBarn).isNull()
                assertThat(barn.saksbehandlerOppgitteBarn?.barn).hasSize(1)
                assertThat(barn.registerbarn?.barn).hasSize(1)

                val tilkjentYtelseFraRepo = hentTilkjentYtelse(behandling.id)

                val tilkjentYtelseTidslinje =
                    tilkjentYtelseFraRepo.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertTrue(tilkjentYtelseTidslinje.isNotEmpty())

                val tilkjentYtelsePeriode = tilkjentYtelseFraRepo.tilTidslinje().helePerioden()
                val barnetillegg = tilkjentYtelseTidslinje.begrensetTil(tilkjentYtelsePeriode)
                assertTidslinje(barnetillegg, tilkjentYtelsePeriode to {
                    assertThat(it.barnetillegg).isEqualTo(Beløp(76))
                    assertThat(it.antallBarn).isEqualTo(2)
                })
            }
    }

    @Test
    fun `Barnetillegg opphører ved registrert dødsdato`() {
        val periode = Periode(LocalDate.of(2026, 1, 1), Tid.MAKS)
        val dødtBarnFødselsdato = periode.fom.minusYears(5)
        val dødtBarnDødsdato = Dødsdato(periode.fom.plusMonths(5))
        val dødtBarnIdent = genererIdent(dødtBarnFødselsdato)
        val dødtBarnNavn = PersonNavn("Kunstig", "Gramatikk")
        val personMedDødtBarn = TestPersoner.STANDARD_PERSON().medBarn(
            listOf(
                TestPerson(
                    identer = setOf(dødtBarnIdent),
                    navn = dødtBarnNavn,
                    fødselsdato = Fødselsdato(dødtBarnFødselsdato),
                    // Dødsdato settes til en dato i fremtiden for å simulere at barnet er dødt
                    dødsdato = dødtBarnDødsdato
                ),
            )
        )

        vurdereFramTilBarnetillegg(TestSøknader.STANDARD_SØKNAD, periode, personMedDødtBarn).second
            .løsBarnetillegg()
            .løsAndreStatligeYtelser()
            .medKontekst {
                val barn = dataSource.transaction {
                    BarnRepositoryImpl(it).hent(behandling.id)
                }
                assertThat(barn.oppgitteBarn).isNull()
                assertThat(barn.saksbehandlerOppgitteBarn).isNull()
                assertThat(barn.registerbarn?.barn).hasSize(1)

                val tilkjentYtelseFraRepo = hentTilkjentYtelse(behandling.id)

                val tilkjentYtelseTidslinje =
                    tilkjentYtelseFraRepo.map { Segment(it.periode, it.tilkjent) }.let(::Tidslinje)

                assertTrue(tilkjentYtelseTidslinje.isNotEmpty())

                val periodeMedBarnetillegg = Periode(periode.fom, dødtBarnDødsdato.toLocalDate())
                val tilkjentYtelseMedBarnetillegg = tilkjentYtelseTidslinje.begrensetTil(periodeMedBarnetillegg)
                assertTidslinje(tilkjentYtelseMedBarnetillegg, periodeMedBarnetillegg to {
                    assertThat(it.barnetillegg).isEqualTo(Beløp(38))
                    assertThat(it.antallBarn).isEqualTo(1)
                })

                val periodeUtenBarnetillegg =
                    Periode(dødtBarnDødsdato.toLocalDate().plusDays(1), tilkjentYtelseTidslinje.maxDato())
                val tilkjentYtelseUtenBarnetillegg = tilkjentYtelseTidslinje.begrensetTil(periodeUtenBarnetillegg)
                assertTidslinje(tilkjentYtelseUtenBarnetillegg, periodeUtenBarnetillegg to {
                    assertThat(it.barnetillegg).isEqualTo(Beløp(0))
                    assertThat(it.antallBarn).isEqualTo(0)
                })
            }

    }

    private fun BehandlingInfo.hentTilkjentYtelse(behandlingId: BehandlingId) =
        requireNotNull(
            repositoryProvider.provide<TilkjentYtelseRepository>().hentHvisEksisterer(behandlingId)
        ) { "Tilkjent ytelse skal være beregnet her." }

    fun lagTestPerson(fornavn: String, etternavn: String, fødselsdato: LocalDate): TestPerson {
        return TestPerson(
            navn = PersonNavn(fornavn, etternavn),
            fødselsdato = Fødselsdato(fødselsdato)
        )
    }

    fun vurdereFramTilBarnetillegg(søknad: SøknadV0, periode: Periode, person: TestPerson): Pair<Sak, Behandling> {
        // Sender inn en søknad
        var (sak, behandling) = sendInnFørsteSøknad(
            mottattTidspunkt = periode.fom.atStartOfDay(),
            periode = periode,
            søknad = søknad,
            person = person
        )

        assertThat(behandling.typeBehandling()).isEqualTo(TypeBehandling.Førstegangsbehandling)
        behandling = behandling.medKontekst {
            assertThat(åpneAvklaringsbehov).isNotEmpty()
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }
            .løsSykdom(periode.fom)
            .løsBistand(periode.fom)
            .løsRefusjonskrav(periode.fom)
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsAvklaringsBehov(
                FastsettBeregningstidspunktLøsning(
                    beregningVurdering = BeregningstidspunktVurderingDto(
                        begrunnelse = "Trenger hjelp fra Nav",
                        nedsattArbeidsevneDato = LocalDate.now(),
                        ytterligereNedsattArbeidsevneDato = null,
                        ytterligereNedsattBegrunnelse = null
                    ),
                ),
            )
            .løsOppholdskrav(periode.fom)

        return sak to behandling
    }

}