package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktId
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.behandlingsflyt.test.mai
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

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(1L, emptyList())

        val samordningsgrunnlag = SamordningGrunnlag(0L, emptyList())

        val samordningUføre = SamordningUføreGrunnlag(vurdering = SamordningUføreVurdering("", emptyList()))

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.of(2023, 4, 30), LocalDate.of(2023, 4, 30)), verdi = Tilkjent(
                    dagsats = Beløp("1131.92"), //4*0.66*111477/260
                    gradering = TilkjentGradering(
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlag = Beløp("1131.92"),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("111477"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            ), Segment(
                periode = Periode(LocalDate.of(2023, 5, 1), LocalDate.of(2023, 5, 1)), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260
                    gradering = TilkjentGradering(
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlag = Beløp("1204.45"),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620.00"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
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
            1L, listOf(
                BarnetilleggPeriode(
                    Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1).plusYears(18)),
                    setOf(Ident("12345678910"))
                )
            )
        )

        val samordningsgrunnlag = SamordningGrunnlag(0L, emptyList())
        val samordningUføre = SamordningUføreGrunnlag(SamordningUføreVurdering("", emptyList()))

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = periode, verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260+36
                    gradering = TilkjentGradering(
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlag = Beløp("1204.45"),
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
            1L, listOf(
                BarnetilleggPeriode(
                    Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1).plusYears(18)),
                    setOf(Ident("12345678910"))
                ), BarnetilleggPeriode(
                    Periode(LocalDate.of(2023, 12, 30).minusYears(18), LocalDate.of(2023, 12, 31)),
                    setOf(Ident("12345678911"))
                )
            )
        )

        val samordningsgrunnlag = SamordningGrunnlag(0L, emptyList())
        val samordningUføre = SamordningUføreGrunnlag(vurdering = SamordningUføreVurdering("", emptyList()))

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.of(2023, 12, 30), LocalDate.of(2023, 12, 31)), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260+36
                    gradering = TilkjentGradering(
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlag = Beløp("1204.45"),
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
                    gradering = TilkjentGradering(
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlag = Beløp("1204.45"),
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

        val samordningsgrunnlag = SamordningGrunnlag(0L, emptyList())

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(1L, emptyList())
        val samordningUføre = SamordningUføreGrunnlag(vurdering = SamordningUføreVurdering("", emptyList()))

        val beregnetTilkjentYtelse = BeregnTilkjentYtelseService(
            fødeselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre
        ).beregnTilkjentYtelse()

        assertThat(beregnetTilkjentYtelse.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.of(2024, 6, 30), LocalDate.of(2024, 6, 30)), verdi = Tilkjent(
                    dagsats = Beløp("954.06"), //118620*2/260
                    gradering = TilkjentGradering(
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlag = Beløp("954.06"),
                    grunnlagsfaktor = GUnit("0.0076923077"),
                    grunnbeløp = Beløp("124028"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)

                )
            ), Segment(
                periode = Periode(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 1)), verdi = Tilkjent(
                    dagsats = Beløp("973.62"), // 124_028 * 2.041/260
                    gradering = TilkjentGradering(
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlag = Beløp("973.62"),
                    grunnlagsfaktor = GUnit("0.0078500000"),
                    grunnbeløp = Beløp("124028"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
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
            1L, emptyList()
        )

        val samordningsgrunnlag = SamordningGrunnlag(0L, emptyList())
        val samordningUføre = SamordningUføreGrunnlag(vurdering = SamordningUføreVurdering("", emptyList()))

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.of(2020, 3, 31), LocalDate.of(2020, 3, 31)), verdi = Tilkjent(
                    dagsats = Beløp("512.09"), //2*2/3*99858/260
                    gradering = TilkjentGradering(
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlag = Beløp("512.09"),
                    grunnlagsfaktor = GUnit("0.0051282051"),
                    grunnbeløp = Beløp("99858"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)

                )
            ), Segment(
                periode = Periode(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 1)), verdi = Tilkjent(
                    dagsats = Beløp("768.14"), //2*99858/260
                    gradering = TilkjentGradering(
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlag = Beløp("768.14"),
                    grunnlagsfaktor = GUnit("0.0076923077"),
                    grunnbeløp = Beløp("99858"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
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

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(1L, emptyList())
        val samordningsgrunnlag = SamordningGrunnlag(0L, listOf())
        val samordningUføre = SamordningUføreGrunnlag(vurdering = SamordningUføreVurdering("", emptyList()))

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(1 juni 2023, 1 august 2023), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*111477/260
                    gradering = TilkjentGradering(
                        endeligGradering = Prosent.`50_PROSENT`,
                        samordningGradering = Prosent.`0_PROSENT`,
                        institusjonGradering = Prosent.`50_PROSENT`,
                        arbeidGradering = Prosent.`100_PROSENT`,
                        samordningUføregradering = Prosent.`0_PROSENT`
                    ),
                    grunnlag = Beløp("1204.45"),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
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

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(1L, emptyList())

        // Samordning-perioden overlapper delvis med perioden det beregnes for
        val samordningsgrunnlag = SamordningGrunnlag(
            0L, listOf(
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
                )
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre
        ).beregnTilkjentYtelse()

        // Forventer 30 prosent grunnlag først, deretter 100 prosent
        assertThat(
            beregnTilkjentYtelseService.segmenter().map { it.verdi.gradering.endeligGradering }).containsExactly(
            Prosent.`30_PROSENT`, Prosent.`0_PROSENT`, Prosent.`100_PROSENT`
        )

        assertThat(beregnTilkjentYtelseService.segmenter()).hasSize(3)
        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(1 juni 2023, 30 juni 2023), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*111477/260
                    gradering = TilkjentGradering(
                        Prosent.`30_PROSENT`,
                        Prosent.`70_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlag = Beløp("1204.45"),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            ),
            Segment(
                periode = Periode(1 juli 2023, 1 august 2023), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*111477/260
                    gradering = TilkjentGradering(
                        Prosent.`0_PROSENT`,
                        Prosent.`70_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`30_PROSENT`
                    ),
                    grunnlag = Beløp("1204.45"),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            ), Segment(
                periode = Periode(2 august 2023, 1 september 2023), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260
                    gradering = TilkjentGradering(
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlag = Beløp("1204.45"),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620.00"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
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

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(1L, emptyList())

        // Samordning-perioden overlapper delvis med perioden det beregnes for
        val samordningsgrunnlag = SamordningGrunnlag(
            0L, emptyList(),
        )

        val samordningUføre = SamordningUføreGrunnlag(
            vurdering = SamordningUføreVurdering(
                "", listOf(
                    SamordningUføreVurderingPeriode(virkningstidspunkt = 1 mars 2023, Prosent.`50_PROSENT`),
                    SamordningUføreVurderingPeriode(virkningstidspunkt = 1 juli 2023, Prosent.`70_PROSENT`)
                )
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre
        ).beregnTilkjentYtelse()

        // Forventer 30 prosent grunnlag først, deretter 100 prosent
        assertThat(
            beregnTilkjentYtelseService.segmenter().map { it.verdi.gradering.endeligGradering }).containsExactly(
            Prosent.`50_PROSENT`, Prosent.`30_PROSENT`
        )

        assertThat(beregnTilkjentYtelseService.segmenter()).hasSize(2)
        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(1 juni 2023, 30 juni 2023), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*111477/260
                    gradering = TilkjentGradering(
                        Prosent.`50_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`50_PROSENT`
                    ),
                    grunnlag = Beløp("1204.45"),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9)
                )
            ), Segment(
                periode = Periode(1 juli 2023, 1 august 2023), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260
                    gradering = TilkjentGradering(
                        Prosent.`30_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`100_PROSENT`,
                        Prosent.`70_PROSENT`
                    ),
                    grunnlag = Beløp("1204.45"),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620.00"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
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
        val barnetilleggGrunnlag = BarnetilleggGrunnlag(1L, emptyList())
        val samordningsgrunnlag = SamordningGrunnlag(0L, emptyList())
        val samordningUføre = SamordningUføreGrunnlag(vurdering = SamordningUføreVurdering("", emptyList()))

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(1 juni 2023, 1 august 2023),
                verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*111477/260
                    gradering = TilkjentGradering(
                        Prosent.`70_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`70_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlag = Beløp("1204.45"),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
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
        val barnetilleggGrunnlag = BarnetilleggGrunnlag(1L, emptyList())
        val samordningsgrunnlag = SamordningGrunnlag(
            0L,
            listOf(
                SamordningPeriode(
                    periode = Periode(1 juni 2023, 1 august 2023),
                    gradering = Prosent.`50_PROSENT`
                )
            )
        )

        val samordningUføre = null

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(1 juni 2023, 1 august 2023),
                verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*111477/260
                    gradering = TilkjentGradering(
                        Prosent(20),
                        Prosent.`50_PROSENT`,
                        Prosent.`0_PROSENT`,
                        Prosent.`70_PROSENT`,
                        Prosent.`0_PROSENT`
                    ),
                    grunnlag = Beløp("1204.45"),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
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
                Underveisperiode(
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
                        opplysningerMottatt = null,
                    ),
                    trekk = Dagsatser(0),
                    brukerAvKvoter = setOf(Kvote.ORDINÆR),
                    bruddAktivitetspliktId = BruddAktivitetspliktId(1),
                    institusjonsoppholdReduksjon = institusjonsOppholdReduksjon,
                    meldepliktStatus = MeldepliktStatus.MELDT_SEG,
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

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(1L, emptyList())
        val samordningsgrunnlag = SamordningGrunnlag(
            0L, listOf(
                SamordningPeriode(
                    periode = periode, gradering = Prosent(10)
                )
            )
        )

        val samordningUføre = SamordningUføreGrunnlag(
            vurdering = SamordningUføreVurdering(
                "", listOf(
                    SamordningUføreVurderingPeriode(virkningstidspunkt = periode.fom, Prosent.`30_PROSENT`)
                )
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag,
            samordningsgrunnlag,
            samordningUføre
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(1 juni 2023, 1 august 2023), verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*111477/260
                    gradering = TilkjentGradering(
                        endeligGradering = Prosent.`30_PROSENT`,
                        samordningGradering = Prosent(10),
                        institusjonGradering = Prosent.`50_PROSENT`,
                        arbeidGradering = Prosent.`100_PROSENT`,
                        samordningUføregradering = Prosent.`30_PROSENT`
                    ),
                    grunnlag = Beløp("1204.45"),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
                    barnetillegg = Beløp("0"),
                    utbetalingsdato = periode.tom.plusDays(9),
                )
            ),
        )
    }

}
