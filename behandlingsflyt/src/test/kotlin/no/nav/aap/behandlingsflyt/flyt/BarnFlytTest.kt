package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBarnetilleggLøsning
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Barn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.BarnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.RegisterBarn
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingAvForeldreAnsvarDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurderingerForBarnetillegg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.VurdertBarnDto
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManueltOppgittBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppgitteBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.underveis.UnderveisRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.barn.BarnRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.pip.PipRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.collections.map
import kotlin.collections.orEmpty

class BarnFlytTest: AbstraktFlytOrkestratorTest(FakeUnleash::class) {
    @Test
    fun `barnetillegg gis fram til 18 år`() {
        val fom = LocalDate.now().minusMonths(3)
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

        val (_, behandling) = sendInnFørsteSøknad(
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
            .løsForutgåendeMedlemskap(fom)
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
        assertTidslinje(barnErAtten, periodeBarnUnderAtten to {
            assertThat(it).isEqualTo(Beløp(37))
        })

        val periodeBarnOverAtten = Periode(barnBlirAttenPå, uthentetTilkjentYtelse.maxOf { it.periode.tom })
        val barnErOverAtten = barnetillegg.begrensetTil(periodeBarnOverAtten)
        assertThat(barnErOverAtten.segmenter()).isNotEmpty
        // Verifiser at barnetillegg er null etter fylte 18 år
        assertTidslinje(barnErOverAtten, periodeBarnOverAtten to {
            assertThat(it).isEqualTo(Beløp(0))
        })
    }

    @Test
    fun `barnetillegg gis ikke for gamle barn`() {
        val fom = LocalDate.now()
        val periode = Periode(fom, Tid.MAKS)

        val ungtBarnFødselsdato = LocalDate.now().minusYears(7)
        val gammeltBarnFødselsdato = LocalDate.now().minusYears(20)

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
            .løsRefusjonskrav()
            .løsSykdomsvurderingBrev()
            .kvalitetssikreOk()
            .løsBeregningstidspunkt()
            .løsForutgåendeMedlemskap(fom)
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
        assertTidslinje(barnetillegg, tilkjentYtelsePeriode to {
            assertThat(it.barnetillegg).isEqualTo(Beløp(37))
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
            .løsForutgåendeMedlemskap(fom)
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
                    dataSource.transaction { TilkjentYtelseRepositoryImpl(it).hentHvisEksisterer(behandling.id) }
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
                val underveisPeriode = dataSource.transaction { connection ->
                    UnderveisRepositoryImpl(connection).hent(behandling.id)
                }.somTidslinje().helePerioden()
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

}