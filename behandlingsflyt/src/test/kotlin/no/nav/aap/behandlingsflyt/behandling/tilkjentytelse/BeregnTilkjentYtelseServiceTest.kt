package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnIdentifikator
import no.nav.aap.behandlingsflyt.test.FakeUnleash
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.behandlingsflyt.test.september
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import java.time.LocalDate

class BeregnTilkjentYtelseServiceTest {

    @Test
    fun `årlig ytelse beregnes til 66 prosent av grunnlaget og dagsatsen er lik årlig ytelse delt på 260, og sjekker split av periode ved endring i Grunnbeløp`() {
        val fødselsdato = Fødselsdato(LocalDate.of(1985, 1, 2))
        val beregningsgrunnlag = Grunnlag11_19(
            grunnlaget = GUnit(BigDecimal(4)),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(0),
            inntekter = emptyList()
        )
        val periode = Periode(LocalDate.of(2023, 4, 30), LocalDate.of(2023, 5, 1))

        val underveisgrunnlag = underveisgrunnlag(periode)

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(emptyList())

        val samordningsgrunnlag = SamordningGrunnlag(emptySet())

        val samordningUføre = SamordningUføreGrunnlag(vurdering = SamordningUføreVurdering("", emptyList(), "ident"))

        val samordningArbeidsgiver = SamordningArbeidsgiverGrunnlag(
            vurdering = SamordningArbeidsgiverVurdering(
                "",
                LocalDate.now(), LocalDate.now(), vurdertAv = "ident"
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre,
            samordningArbeidsgiver,
            FakeUnleash
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.of(2023, 4, 30), LocalDate.of(2023, 4, 30)), verdi = Tilkjent(
                    dagsats = Beløp("1131.92"), //4*0.66*111477/260
                    gradering = Prosent.`100_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("111477"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("35.00"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            ), Segment(
                periode = Periode(LocalDate.of(2023, 5, 1), LocalDate.of(2023, 5, 1)), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260
                    gradering = Prosent.`100_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620.00"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("35.00"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            )
        )
    }

    @Test
    fun `bruker får barnetillegg dersom bruker har barn`() {
        val fødselsdato = Fødselsdato(LocalDate.of(1985, 1, 2))
        val beregningsgrunnlag = Grunnlag11_19(
            grunnlaget = GUnit(BigDecimal(4)),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(0),
            inntekter = emptyList()
        )
        val periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1))

        val underveisgrunnlag = underveisgrunnlag(periode)

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(
            listOf(
                BarnetilleggPeriode(
                    Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1).plusYears(18)),
                    setOf(BarnIdentifikator.BarnIdent("12345678910"))
                )
            )
        )

        val samordningsgrunnlag = SamordningGrunnlag(emptySet())
        val samordningUføre = SamordningUføreGrunnlag(SamordningUføreVurdering("", emptyList(), "ident"))

        val samordningArbeidsgiver = SamordningArbeidsgiverGrunnlag(
            vurdering = SamordningArbeidsgiverVurdering(
                "",
                LocalDate.now(), LocalDate.now(), vurdertAv = "ident"
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre,
            samordningArbeidsgiver,
            FakeUnleash
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = periode, verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260+36
                    gradering = Prosent.`100_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 1,
                    barnetilleggsats = Beløp("36"),
                    barnetillegg = Beløp("36"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            )
        )
    }

    @Test
    fun `Hva skjer med etterpåklatt`() {
        val fødselsdato = Fødselsdato(LocalDate.of(1985, 1, 2))
        val beregningsgrunnlag = Grunnlag11_19(
            grunnlaget = GUnit(BigDecimal(4)),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(0),
            inntekter = emptyList()
        )
        val periode = Periode(LocalDate.of(2023, 12, 30), LocalDate.of(2024, 1, 1))
        val underveisgrunnlag = underveisgrunnlag(periode)

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(
            listOf(
                BarnetilleggPeriode(
                    Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1).plusYears(18)),
                    setOf(BarnIdentifikator.BarnIdent("12345678910"))
                ), BarnetilleggPeriode(
                    Periode(LocalDate.of(2023, 12, 30).minusYears(18), LocalDate.of(2023, 12, 31)),
                    setOf(BarnIdentifikator.BarnIdent("12345678911"))
                )
            )
        )

        val samordningsgrunnlag = SamordningGrunnlag(emptySet())
        val samordningUføre = SamordningUføreGrunnlag(vurdering = SamordningUføreVurdering("", emptyList(), "ident"))

        val samordningArbeidsgiver = SamordningArbeidsgiverGrunnlag(
            vurdering = SamordningArbeidsgiverVurdering(
                "",
                LocalDate.now(), LocalDate.now(), vurdertAv = "ident"
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre,
            samordningArbeidsgiver,
            FakeUnleash
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.of(2023, 12, 30), LocalDate.of(2023, 12, 31)), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260+36
                    gradering = Prosent.`100_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 1,
                    barnetilleggsats = Beløp("35"),
                    barnetillegg = Beløp("35"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            ), Segment(
                periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260+36
                    gradering = Prosent.`100_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 1,
                    barnetilleggsats = Beløp("36"),
                    barnetillegg = Beløp("36"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            )
        )
    }

    @Test
    fun `minste årlige ytelse er lik 2G før 1 juli 2024 og lik 2,041G fom 1 juli 2024`() {
        // Denne må oppdateres når grunnbeløper endres 1. mai 2025
        val fødeselsdato = Fødselsdato(LocalDate.of(1985, 4, 1))
        val beregningsgrunnlag = Grunnlag11_19(
            grunnlaget = GUnit(BigDecimal(0)),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(0),
            inntekter = emptyList()
        )
        val periode = Periode(LocalDate.of(2024, 6, 30), LocalDate.of(2024, 7, 1))
        val underveisgrunnlag = underveisgrunnlag(periode)

        val samordningsgrunnlag = SamordningGrunnlag(emptySet())

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(emptyList())
        val samordningUføre = SamordningUføreGrunnlag(vurdering = SamordningUføreVurdering("", emptyList(), "ident"))

        val samordningArbeidsgiver = SamordningArbeidsgiverGrunnlag(
            vurdering = SamordningArbeidsgiverVurdering(
                "",
                LocalDate.now(), LocalDate.now(), vurdertAv = "ident"
            )
        )

        val beregnetTilkjentYtelse = BeregnTilkjentYtelseService(
            fødeselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre,
            samordningArbeidsgiver,
            FakeUnleash
        ).beregnTilkjentYtelse()

        assertThat(beregnetTilkjentYtelse.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.of(2024, 6, 30), LocalDate.of(2024, 6, 30)), verdi = Tilkjent(
                    dagsats = Beløp("954.06"), //118620*2/260
                    gradering = Prosent.`100_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0076923077"),
                    grunnbeløp = Beløp("124028"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("36.00"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)

                )
            ), Segment(
                periode = Periode(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 1)), verdi = Tilkjent(
                    dagsats = Beløp("973.62"), // 124_028 * 2.041/260
                    gradering = Prosent.`100_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0078500000"),
                    grunnbeløp = Beløp("124028"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("36.00"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            )
        )
    }

    @Test
    fun `Minste Årlig Ytelse justeres ift alder`() {
        val fødselsdato = Fødselsdato(LocalDate.of(1995, 4, 1))
        val beregningsgrunnlag = Grunnlag11_19(
            grunnlaget = GUnit(BigDecimal(0)),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(0),
            inntekter = emptyList()
        )
        val periode = Periode(LocalDate.of(2020, 3, 31), LocalDate.of(2020, 4, 1))
        val underveisgrunnlag = underveisgrunnlag(periode)
        val barnetilleggGrunnlag = BarnetilleggGrunnlag(
            emptyList()
        )

        val samordningsgrunnlag = SamordningGrunnlag(emptySet())
        val samordningUføre = SamordningUføreGrunnlag(vurdering = SamordningUføreVurdering("", emptyList(), "ident"))

        val samordningArbeidsgiver = SamordningArbeidsgiverGrunnlag(
            vurdering = SamordningArbeidsgiverVurdering(
                "",
                LocalDate.now(), LocalDate.now(), vurdertAv = "ident"
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre,
            samordningArbeidsgiver,
            FakeUnleash
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.of(2020, 3, 31), LocalDate.of(2020, 3, 31)), verdi = Tilkjent(
                    dagsats = Beløp("512.09"), //2*2/3*99858/260
                    gradering = Prosent.`100_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0051282051"),
                    grunnbeløp = Beløp("99858"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("27.00"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)

                )
            ), Segment(
                periode = Periode(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 1)), verdi = Tilkjent(
                    dagsats = Beløp("768.14"), //2*99858/260
                    gradering = Prosent.`100_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0076923077"),
                    grunnbeløp = Beløp("99858"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("27.00"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            )
        )
    }

    @Test
    fun `justerer institusjonsopphold med prosent, ikke prosent-poeng`() {
        val fødselsdato = Fødselsdato(LocalDate.of(1985, 1, 2))
        val beregningsgrunnlag = object : Grunnlag {
            override fun grunnlaget(): GUnit {
                return GUnit(BigDecimal(4))
            }
        }
        val periode = Periode(1 juni 2023, 1 august 2023)

        val underveisgrunnlag = underveisgrunnlag(periode, institusjonsOppholdReduksjon = Prosent.`50_PROSENT`)

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(emptyList())
        val samordningsgrunnlag = SamordningGrunnlag(emptySet())
        val samordningUføre = SamordningUføreGrunnlag(vurdering = SamordningUføreVurdering("", emptyList(), "ident"))

        val samordningArbeidsgiver = SamordningArbeidsgiverGrunnlag(
            vurdering = SamordningArbeidsgiverVurdering(
                "",
                LocalDate.now(), LocalDate.now(), vurdertAv = "ident"
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre,
            samordningArbeidsgiver,
            FakeUnleash
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(1 juni 2023, 1 august 2023), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*111477/260
                    gradering = Prosent.`50_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        samordningGradering = Prosent.`0_PROSENT`,
                        institusjonGradering = Prosent.`50_PROSENT`,
                        arbeidGradering = Prosent.`100_PROSENT`,
                        samordningUføregradering = Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("35.00"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9),
                )
            ),
        )
    }

    @Test
    fun `graderer tilkjent ytelse med samordning-graderinger`() {
        val fødselsdato = Fødselsdato(LocalDate.of(1985, 1, 2))
        val beregningsgrunnlag = object : Grunnlag {
            override fun grunnlaget(): GUnit {
                return GUnit(BigDecimal(4))
            }
        }
        val periode = Periode(1 juni 2023, 1 september 2023)

        val underveisgrunnlag = underveisgrunnlag(periode)

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(emptyList())

        // Samordning-perioden overlapper delvis med perioden det beregnes for
        val samordningsgrunnlag = SamordningGrunnlag(
            setOf(
                SamordningPeriode(
                    periode = Periode(1 mars 2023, 1 august 2023), gradering = Prosent.`70_PROSENT`
                )
            )
        )

        val samordningUføre = SamordningUføreGrunnlag(
            vurdering = SamordningUføreVurdering(
                "", listOf(
                    SamordningUføreVurderingPeriode(virkningstidspunkt = 1 juli 2023, Prosent.`30_PROSENT`),
                    SamordningUføreVurderingPeriode(virkningstidspunkt = 2 august 2023, Prosent.`0_PROSENT`)
                ),
                "ident"
            )
        )

        val samordningArbeidsgiver = SamordningArbeidsgiverGrunnlag(
            vurdering = SamordningArbeidsgiverVurdering(
                "",
                LocalDate.now(), LocalDate.now(), vurdertAv = "ident"
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre,
            samordningArbeidsgiver,
            FakeUnleash
        ).beregnTilkjentYtelse()

        // Forventer 30 prosent grunnlag først, deretter 100 prosent
        assertThat(
            beregnTilkjentYtelseService.segmenter().map { it.verdi.gradering }).containsExactly(
            Prosent.`30_PROSENT`, Prosent.`0_PROSENT`, Prosent.`100_PROSENT`
        )

        assertThat(beregnTilkjentYtelseService.segmenter()).hasSize(3)
        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(1 juni 2023, 30 juni 2023), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*111477/260
                    gradering = Prosent.`30_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        Prosent.`70_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("35.00"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            ),
            Segment(
                periode = Periode(1 juli 2023, 1 august 2023), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*111477/260
                    gradering = Prosent.`0_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        Prosent.`70_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`30_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("35.00"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            ), Segment(
                periode = Periode(2 august 2023, 1 september 2023), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260
                    gradering = Prosent.`100_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620.00"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("35.00"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            )
        )
    }

    @Test
    fun `graderer tilkjent ytelse med samordning uføre`() {
        val fødselsdato = Fødselsdato(LocalDate.of(1985, 1, 2))
        val beregningsgrunnlag = object : Grunnlag {
            override fun grunnlaget(): GUnit {
                return GUnit(BigDecimal(4))
            }
        }
        val periode = Periode(1 juni 2023, 1 august 2023)

        val underveisgrunnlag = underveisgrunnlag(periode)

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(emptyList())

        // Samordning-perioden overlapper delvis med perioden det beregnes for
        val samordningsgrunnlag = SamordningGrunnlag(
            setOf(),
        )

        val samordningUføre = SamordningUføreGrunnlag(
            vurdering = SamordningUføreVurdering(
                "", listOf(
                    SamordningUføreVurderingPeriode(virkningstidspunkt = 1 mars 2023, Prosent.`50_PROSENT`),
                    SamordningUføreVurderingPeriode(virkningstidspunkt = 1 juli 2023, Prosent.`70_PROSENT`)
                ),
                "ident"
            )
        )

        val samordningArbeidsgiver = SamordningArbeidsgiverGrunnlag(
            vurdering = SamordningArbeidsgiverVurdering(
                "",
                LocalDate.now(), LocalDate.now(), vurdertAv = "ident"
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre,
            samordningArbeidsgiver,
            FakeUnleash
        ).beregnTilkjentYtelse()

        // Forventer 30 prosent grunnlag først, deretter 100 prosent
        assertThat(
            beregnTilkjentYtelseService.segmenter().map { it.verdi.gradering }).containsExactly(
            Prosent.`50_PROSENT`, Prosent.`30_PROSENT`
        )

        assertThat(beregnTilkjentYtelseService.segmenter()).hasSize(2)
        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(1 juni 2023, 30 juni 2023), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*111477/260
                    gradering = Prosent.`50_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`50_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("35.00"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            ), Segment(
                periode = Periode(1 juli 2023, 1 august 2023), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260
                    gradering = Prosent.`30_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`70_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620.00"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("35.00"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            )
        )
    }


    @Test
    fun `arbeidsgrad reduserer tilkjent endelig utbetalingsgrad`() {
        val fødselsdato = Fødselsdato(LocalDate.of(1985, 1, 2))
        val beregningsgrunnlag = object : Grunnlag {
            override fun grunnlaget(): GUnit {
                return GUnit(BigDecimal(4))
            }
        }
        val periode = Periode(1 juni 2023, 1 august 2023)

        val underveisgrunnlag = underveisgrunnlag(periode, gradering = Prosent.`70_PROSENT`)
        val barnetilleggGrunnlag = BarnetilleggGrunnlag(emptyList())
        val samordningsgrunnlag = SamordningGrunnlag(emptySet())
        val samordningUføre = SamordningUføreGrunnlag(vurdering = SamordningUføreVurdering("", emptyList(), "ident"))

        val samordningArbeidsgiver = SamordningArbeidsgiverGrunnlag(
            vurdering = SamordningArbeidsgiverVurdering(
                "",
                LocalDate.now(), LocalDate.now(), vurdertAv = "ident"
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre,
            samordningArbeidsgiver,
            FakeUnleash
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(1 juni 2023, 1 august 2023),
                verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*111477/260
                    gradering = Prosent.`70_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        samordningGradering = Prosent.`0_PROSENT`,
                        institusjonGradering = Prosent.`0_PROSENT`,
                        arbeidGradering = Prosent.`70_PROSENT`,
                        samordningUføregradering = Prosent.`0_PROSENT`,
                        samordningArbeidsgiverGradering = Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("35.00"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            )
        )
    }

    @Test
    fun `arbeidsgrad og samordning reduserer tilkjent endelig utbetalingsgrad`() {
        val fødselsdato = Fødselsdato(LocalDate.of(1985, 1, 2))
        val beregningsgrunnlag = object : Grunnlag {
            override fun grunnlaget(): GUnit {
                return GUnit(BigDecimal(4))
            }
        }
        val periode = Periode(1 juni 2023, 1 august 2023)

        val underveisgrunnlag = underveisgrunnlag(periode, gradering = Prosent.`70_PROSENT`)
        val barnetilleggGrunnlag = BarnetilleggGrunnlag(emptyList())
        val samordningsgrunnlag = SamordningGrunnlag(
            setOf(
                SamordningPeriode(
                    periode = Periode(1 juni 2023, 1 august 2023),
                    gradering = Prosent.`50_PROSENT`
                )
            )
        )

        val samordningUføre = null

        val samordningArbeidsgiver = SamordningArbeidsgiverGrunnlag(
            vurdering = SamordningArbeidsgiverVurdering(
                "",
                LocalDate.now(), LocalDate.now(), vurdertAv = "ident"
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre,
            samordningArbeidsgiver,
            FakeUnleash
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(1 juni 2023, 1 august 2023),
                verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*111477/260
                    gradering = Prosent(20),
                    graderingGrunnlag = GraderingGrunnlag(
                        Prosent.`50_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`70_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                    ),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("35.00"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            )
        )
    }

    private fun underveisgrunnlag(
        periode: Periode,
        institusjonsOppholdReduksjon: Prosent = Prosent.`0_PROSENT`,
        gradering: Prosent = Prosent.`100_PROSENT`
    ): UnderveisGrunnlag {
        return UnderveisGrunnlag(
            id = 1L, perioder = listOf(
                underveisperiode(periode, gradering, institusjonsOppholdReduksjon)
            )
        )
    }

    private fun underveisperiode(
        periode: Periode,
        gradering: Prosent,
        institusjonsOppholdReduksjon: Prosent,
        meldepliktStatus: MeldepliktStatus = MeldepliktStatus.MELDT_SEG,
        opplysningerMottatt: LocalDate? = null,
    ): Underveisperiode = Underveisperiode(
        periode = periode,
        meldePeriode = periode,
        utfall = Utfall.OPPFYLT,
        rettighetsType = RettighetsType.BISTANDSBEHOV,
        avslagsårsak = null,
        grenseverdi = Prosent.`100_PROSENT`,
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(BigDecimal(10)),
            andelArbeid = Prosent.`50_PROSENT`,
            fastsattArbeidsevne = Prosent.`50_PROSENT`,
            gradering = gradering,
            opplysningerMottatt = opplysningerMottatt,
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = setOf(Kvote.ORDINÆR),
        institusjonsoppholdReduksjon = institusjonsOppholdReduksjon,
        meldepliktStatus = meldepliktStatus,
    )

    @Test
    fun `sluttpakke fra arbeidsgiver reduserer tilkjent endelig utbetalingsgrad`() {
        val fødselsdato = Fødselsdato(LocalDate.of(1985, 1, 2))
        val beregningsgrunnlag = object : Grunnlag {
            override fun grunnlaget(): GUnit {
                return GUnit(BigDecimal(4))
            }
        }
        val periode = Periode(1 juni 2023, 1 august 2023)

        val underveisgrunnlag = underveisgrunnlag(periode, gradering = Prosent.`70_PROSENT`)
        val barnetilleggGrunnlag = BarnetilleggGrunnlag(emptyList())
        val samordningsgrunnlag = SamordningGrunnlag(emptySet())
        val samordningUføre = SamordningUføreGrunnlag(vurdering = SamordningUføreVurdering("", emptyList(), "ident"))

        val samordningArbeidsgiver = SamordningArbeidsgiverGrunnlag(
            vurdering = SamordningArbeidsgiverVurdering(
                "Har fått sluttpakke",
                LocalDate.of(2023, 6, 1), LocalDate.of(2023, 8, 1), vurdertAv = "ident"
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre,
            samordningArbeidsgiver,
            FakeUnleash
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(1 juni 2023, 1 august 2023),
                verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*111477/260
                    gradering = Prosent.`0_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`70_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("35.00"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            )
        )
    }

    @Test
    fun `justerer institusjonsopphold etter å ha samordnet uføre og sykepenger`() {
        val fødselsdato = Fødselsdato(LocalDate.of(1985, 1, 2))
        val beregningsgrunnlag = object : Grunnlag {
            override fun grunnlaget(): GUnit {
                return GUnit(BigDecimal(4))
            }
        }
        val periode = Periode(1 juni 2023, 1 august 2023)

        val underveisgrunnlag = underveisgrunnlag(periode, institusjonsOppholdReduksjon = Prosent.`50_PROSENT`)

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(emptyList())
        val samordningsgrunnlag = SamordningGrunnlag(
            setOf(
                SamordningPeriode(
                    periode = periode, gradering = Prosent(10)
                )
            )
        )

        val samordningUføre = SamordningUføreGrunnlag(
            vurdering = SamordningUføreVurdering(
                "", listOf(
                    SamordningUføreVurderingPeriode(virkningstidspunkt = periode.fom, Prosent.`30_PROSENT`)
                ),
                "ident"
            )
        )

        val samordningArbeidsgiver = SamordningArbeidsgiverGrunnlag(
            vurdering = SamordningArbeidsgiverVurdering(
                "",
                LocalDate.now(), LocalDate.now(), vurdertAv = "ident"
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre,
            samordningArbeidsgiver,
            FakeUnleash
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(1 juni 2023, 1 august 2023), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*111477/260
                    gradering = Prosent.`30_PROSENT`,
                    graderingGrunnlag = GraderingGrunnlag(
                        samordningGradering = Prosent(10),
                        institusjonGradering = Prosent.`50_PROSENT`,
                        arbeidGradering = Prosent.`100_PROSENT`,
                        samordningUføregradering = Prosent.`30_PROSENT`,
                        samordningArbeidsgiverGradering = Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("35.00"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9),
                )
            ),
        )
    }


    // https://confluence.adeo.no/spaces/PAAP/pages/720916013/Utbetalingsgrad
    @CsvSource(
        useHeadersInDisplayName = true,
        textBlock = """arbeidsgrad,	sykepengegrad,	uforegrad,	institusjon,	   effektivGradering
25	               ,25	         ,25  	      ,0	                  ,25
0	               ,50	         ,0  	      ,50	                  ,25
10	               ,25	         ,25	      ,50	                  ,20
50	               ,0	         ,0	          ,50	                  ,25
0	               ,50	         ,50	      ,0	                  ,0
0	               ,50	         ,50	      ,50	                  ,0
50	               ,30	         ,0	          ,50	                  ,10
0	               ,10	         ,30	      ,50	                  ,30
"""
    )
    @ParameterizedTest
    fun `mange test caser`(
        arbeidsgrad: Int,
        sykepengegrad: Int,
        uforegrad: Int,
        institusjon: Int,
        effektivGradering: Double
    ) {
        val fødselsdato = Fødselsdato(LocalDate.of(1985, 1, 2))
        val beregningsgrunnlag = object : Grunnlag {
            override fun grunnlaget(): GUnit {
                return GUnit(BigDecimal(4))
            }
        }
        val periode = Periode(1 juni 2023, 1 august 2023)

        val underveisgrunnlag = underveisgrunnlag(
            periode,
            institusjonsOppholdReduksjon = Prosent(institusjon),
            // Komplement siden gradering = 100% - hvor mange prosent arbeid.
            gradering = Prosent(arbeidsgrad).komplement(),
        )

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(emptyList())
        val samordningsgrunnlag = SamordningGrunnlag(
            setOf(
                SamordningPeriode(
                    periode = periode, gradering = Prosent(sykepengegrad)
                )
            )
        )

        val samordningUføre = SamordningUføreGrunnlag(
            vurdering = SamordningUføreVurdering(
                "", listOf(
                    SamordningUføreVurderingPeriode(virkningstidspunkt = periode.fom, Prosent(uforegrad))
                ),
                "ident"
            )
        )

        val samordningArbeidsgiver = SamordningArbeidsgiverGrunnlag(
            vurdering = SamordningArbeidsgiverVurdering(
                "",
                LocalDate.now(), LocalDate.now(), vurdertAv = "ident"
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre,
            samordningArbeidsgiver,
            FakeUnleash
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(1 juni 2023, 1 august 2023), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*111477/260
                    gradering = Prosent(effektivGradering.toInt()),
                    graderingGrunnlag = GraderingGrunnlag(
                        samordningGradering = Prosent(sykepengegrad),
                        institusjonGradering = Prosent(institusjon),
                        arbeidGradering = Prosent(arbeidsgrad).komplement(),
                        samordningUføregradering = Prosent(uforegrad),
                        samordningArbeidsgiverGradering = Prosent.`0_PROSENT`
                    ),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("35.00"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9),
                )
            ),
        )
    }


    @Test
    fun `skal sette riktig utbetalingsdato basert på ulike meldepliktstatuser - fritak, meldt seg eller før vedtak`() {
        val fødselsdato = Fødselsdato(LocalDate.of(1985, 1, 2))
        val beregningsgrunnlag = Grunnlag11_19(
            grunnlaget = GUnit(BigDecimal(4)),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(0),
            inntekter = emptyList()
        )
        val periode1 = Periode(LocalDate.of(2023, 1,1), LocalDate.of(2023, 1, 14))
        val periode2 = Periode(LocalDate.of(2023, 1, 15), LocalDate.of(2023, 1, 29))
        val periode3 = Periode(LocalDate.of(2023, 1, 30), LocalDate.of(2023, 2, 13))

        val underveisgrunnlag = UnderveisGrunnlag(
            1L, perioder = listOf(
                underveisperiode(periode1, Prosent.`100_PROSENT`, Prosent.`0_PROSENT`, MeldepliktStatus.FØR_VEDTAK),
                underveisperiode(periode2, Prosent.`100_PROSENT`, Prosent.`0_PROSENT`, MeldepliktStatus.FRITAK),
                underveisperiode(periode3, Prosent.`100_PROSENT`, Prosent.`0_PROSENT`, MeldepliktStatus.MELDT_SEG, opplysningerMottatt = periode3.tom.plusDays(1)),
            )
        )

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(emptyList())
        val samordningsgrunnlag = SamordningGrunnlag(emptySet())
        val samordningUføre = SamordningUføreGrunnlag(vurdering = SamordningUføreVurdering("", emptyList(), "ident"))
        val samordningArbeidsgiver = SamordningArbeidsgiverGrunnlag(
            vurdering = SamordningArbeidsgiverVurdering(
                "",
                LocalDate.now(), LocalDate.now(), vurdertAv = "ident"
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre,
            samordningArbeidsgiver,
            FakeUnleash
        ).beregnTilkjentYtelse()

        val tilkjent = Tilkjent(
            dagsats = Beløp("1131.92"), //4*0.66*118620/260
            gradering = Prosent.`100_PROSENT`,
            graderingGrunnlag = GraderingGrunnlag(
                Prosent.`0_PROSENT`,
                Prosent.`0_PROSENT`,
                Prosent.`100_PROSENT`,
                Prosent.`0_PROSENT`,
                Prosent.`0_PROSENT`
            ),
            grunnlagsfaktor = GUnit("0.0101538462"),
            grunnbeløp = Beløp("111477"),
            antallBarn = 0,
            barnetilleggsats = Beløp("27.00"),
            barnetillegg = Beløp("0"),
            utbetalingsdato = LocalDate.now()
        )
        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(periode = periode1, verdi = tilkjent.copy(utbetalingsdato = periode1.tom.plusDays(9))),
            Segment(periode = periode2, verdi = tilkjent.copy(utbetalingsdato = periode2.tom.plusDays(1))),
            Segment(
                periode = Periode(periode3.fom, LocalDate.of(2023, 1, 31)),
                verdi = tilkjent.copy(utbetalingsdato = periode3.tom.plusDays(1))
            ),
            Segment(
                periode = Periode(LocalDate.of(2023, 2, 1), periode3.tom),
                verdi = tilkjent.copy(barnetilleggsats = Beløp("35.00"), utbetalingsdato = periode3.tom.plusDays(1))
            ),
        )
    }

}
