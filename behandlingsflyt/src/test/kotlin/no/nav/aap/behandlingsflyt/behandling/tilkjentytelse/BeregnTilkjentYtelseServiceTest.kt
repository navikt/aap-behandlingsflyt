package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Kvote
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Gradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.BruddAktivitetspliktId
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
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
        val underveisgrunnlag =
            underveisgrunnlag(Periode(LocalDate.of(2023, 4, 30), LocalDate.of(2023, 5, 1)))


        val barnetilleggGrunnlag = BarnetilleggGrunnlag(1L, emptyList())

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.of(2023, 4, 30), LocalDate.of(2023, 4, 30)),
                verdi = Tilkjent(
                    dagsats = Beløp("1131.92"), //4*0.66*111477/260
                    gradering = Prosent.`0_PROSENT`,
                    grunnlag = Beløp("1131.92"),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("111477"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
                    barnetillegg = Beløp("0")
                )
            ),
            Segment(
                periode = Periode(LocalDate.of(2023, 5, 1), LocalDate.of(2023, 5, 1)),
                verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260
                    gradering = Prosent.`0_PROSENT`,
                    grunnlag = Beløp("1204.45"),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620.00"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
                    barnetillegg = Beløp("0")
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
        val underveisgrunnlag = underveisgrunnlag(
            Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1))
        )

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(
            1L,
            listOf(
                BarnetilleggPeriode(
                    Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1).plusYears(18)),
                    setOf(Ident("12345678910"))
                )
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
                verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260+36
                    gradering = Prosent.`0_PROSENT`,
                    grunnlag = Beløp("1204.45"),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 1,
                    barnetilleggsats = Beløp("36"),
                    barnetillegg = Beløp("36")
                )
            )
        )
    }

    @Test
    fun `Hva skjed med etterpåklatt`() {
        val fødselsdato = Fødselsdato(LocalDate.of(1985, 1, 2))
        val beregningsgrunnlag = Grunnlag11_19(
            grunnlaget = GUnit(BigDecimal(4)),
            erGjennomsnitt = false,
            gjennomsnittligInntektIG = GUnit(0),
            inntekter = emptyList()
        )
        val underveisgrunnlag = underveisgrunnlag(Periode(LocalDate.of(2023, 12, 30), LocalDate.of(2024, 1, 1)))

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(
            1L,
            listOf(
                BarnetilleggPeriode(
                    Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1).plusYears(18)),
                    setOf(Ident("12345678910"))
                ),
                BarnetilleggPeriode(
                    Periode(LocalDate.of(2023, 12, 30).minusYears(18), LocalDate.of(2023, 12, 31)),
                    setOf(Ident("12345678911"))
                )
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.of(2023, 12, 30), LocalDate.of(2023, 12, 31)),
                verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260+36
                    gradering = Prosent.`0_PROSENT`,
                    grunnlag = Beløp("1204.45"),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 1,
                    barnetilleggsats = Beløp("35"),
                    barnetillegg = Beløp("35")
                )
            ),
            Segment(
                periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
                verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260+36
                    gradering = Prosent.`0_PROSENT`,
                    grunnlag = Beløp("1204.45"),
                    grunnlagsfaktor = GUnit("0.0101538462"),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 1,
                    barnetilleggsats = Beløp("36"),
                    barnetillegg = Beløp("36")
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
        val underveisgrunnlag = underveisgrunnlag(Periode(LocalDate.of(2024, 6, 30), LocalDate.of(2024, 7, 1)))


        val barnetilleggGrunnlag = BarnetilleggGrunnlag(1L, emptyList())

        val beregnetTilkjentYtelse = BeregnTilkjentYtelseService(
            fødeselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag
        ).beregnTilkjentYtelse()

        assertThat(beregnetTilkjentYtelse.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.of(2024, 6, 30), LocalDate.of(2024, 6, 30)),
                verdi = Tilkjent(
                    dagsats = Beløp("954.06"), //118620*2/260
                    gradering = Prosent.`0_PROSENT`,
                    grunnlag = Beløp("954.06"),
                    grunnlagsfaktor = GUnit("0.0076923077"),
                    grunnbeløp = Beløp("124028"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
                    barnetillegg = Beløp("0")
                )
            ),
            Segment(
                periode = Periode(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 1)),
                verdi = Tilkjent(
                    dagsats = Beløp("973.62"), // 124_028 * 2.041/260
                    gradering = Prosent.`0_PROSENT`,
                    grunnlag = Beløp("973.62"),
                    grunnlagsfaktor = GUnit("0.0078500000"),
                    grunnbeløp = Beløp("124028"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
                    barnetillegg = Beløp("0")
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
        val underveisgrunnlag = underveisgrunnlag(Periode(LocalDate.of(2020, 3, 31), LocalDate.of(2020, 4, 1)))
        val barnetilleggGrunnlag = BarnetilleggGrunnlag(
            1L,
            emptyList()
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.of(2020, 3, 31), LocalDate.of(2020, 3, 31)),
                verdi = Tilkjent(
                    dagsats = Beløp("512.09"), //2*2/3*99858/260
                    gradering = Prosent.`0_PROSENT`,
                    grunnlag = Beløp("512.09"),
                    grunnlagsfaktor = GUnit("0.0051282051"),
                    grunnbeløp = Beløp("99858"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
                    barnetillegg = Beløp("0")
                )
            ),
            Segment(
                periode = Periode(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 1)),
                verdi = Tilkjent(
                    dagsats = Beløp("768.14"), //2*99858/260
                    gradering = Prosent.`0_PROSENT`,
                    grunnlag = Beløp("768.14"),
                    grunnlagsfaktor = GUnit("0.0076923077"),
                    grunnbeløp = Beløp("99858"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("0"),
                    barnetillegg = Beløp("0")
                )
            )
        )
    }

    private fun underveisgrunnlag(periode: Periode): UnderveisGrunnlag {
        return UnderveisGrunnlag(
            id = 1L, perioder = listOf(
                Underveisperiode(
                    periode = periode,
                    meldePeriode = Periode(
                        LocalDate.of(2020, 3, 31), LocalDate.of(2020, 4, 13)
                    ),
                    utfall = Utfall.OPPFYLT,
                    avslagsårsak = null,
                    grenseverdi = Prosent.`100_PROSENT`,
                    gradering = Gradering(
                        totaltAntallTimer = TimerArbeid(BigDecimal(10)),
                        andelArbeid = Prosent.`50_PROSENT`,
                        fastsattArbeidsevne = Prosent.`50_PROSENT`,
                        gradering = Prosent.`0_PROSENT`
                    ),
                    trekk = Dagsatser(0),
                    brukerAvKvoter = setOf(Kvote.ORDINÆR),
                    bruddAktivitetspliktId = BruddAktivitetspliktId(1),
                )
            )
        )
    }
}
