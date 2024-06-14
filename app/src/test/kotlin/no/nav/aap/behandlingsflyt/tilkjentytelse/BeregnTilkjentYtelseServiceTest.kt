package no.nav.aap.behandlingsflyt.tilkjentytelse

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.Tilkjent
import no.nav.aap.tidslinje.Segment
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class BeregnTilkjentYtelseServiceTest {

    @Test
    fun `årlig ytelse beregnes til 66 prosent av grunnlaget og dagsatsen er lik årlig ytelse delt på 260, og sjekker split av periode ved endring i Grunnbeløp`() {
        val fødselsdato = Fødselsdato(LocalDate.of(1985, 1, 2))
        val beregningsgrunnlag = Grunnlag11_19(
            GUnit(BigDecimal(4)),
            false,
            false
        )
        val underveisgrunnlag = UnderveisGrunnlag(
            id = 1L, listOf(
                Underveisperiode(
                    periode = Periode(LocalDate.of(2023, 4, 30), LocalDate.of(2023, 5, 1)),
                    utfall = Utfall.OPPFYLT,
                    avslagsårsak = null,
                    grenseverdi = Prosent.`100_PROSENT`,
                    gradering = null
                )
            )
        )

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
                    grunnlag = Beløp("0"),
                    grunnlagsfaktor = GUnit(0),
                    grunnbeløp = Beløp("99858"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("27"),
                    barnetillegg = Beløp("0")
                )
            ),
            Segment(
                periode = Periode(LocalDate.of(2023, 5, 1), LocalDate.of(2023, 5, 1)),
                verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260
                    gradering = Prosent.`0_PROSENT`,
                    grunnlag = Beløp("0"),
                    grunnlagsfaktor = GUnit(0),
                    grunnbeløp = Beløp("99858"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("27"),
                    barnetillegg = Beløp("0")
                )
            )
        )
    }

    @Test
    fun `bruker får barnetillegg dersom bruker har barn`() {
        val fødselsdato = Fødselsdato(LocalDate.of(1985, 1, 2))
        val beregningsgrunnlag = Grunnlag11_19(
            GUnit(BigDecimal(4)),
            false,
            false
        )
        val underveisgrunnlag = UnderveisGrunnlag(
            id = 1L, listOf(
                Underveisperiode(
                    periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)),
                    utfall = Utfall.OPPFYLT,
                    avslagsårsak = null,
                    grenseverdi = Prosent.`100_PROSENT`,
                    gradering = null
                )
            )
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
                    grunnlag = Beløp("0"),
                    grunnlagsfaktor = GUnit(0),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 1,
                    barnetilleggsats = Beløp("36"),
                    barnetillegg = Beløp("0")
                )
            )
        )
    }

    @Test
    fun `Hva skjed med etterpåklatt`() {
        val fødselsdato = Fødselsdato(LocalDate.of(1985, 1, 2))
        val beregningsgrunnlag = Grunnlag11_19(
            GUnit(BigDecimal(4)),
            false,
            false
        )
        val underveisgrunnlag = UnderveisGrunnlag(
            id = 1L, listOf(
                Underveisperiode(
                    periode = Periode(LocalDate.of(2023, 12, 30), LocalDate.of(2024, 1, 1)),
                    utfall = Utfall.OPPFYLT,
                    avslagsårsak = null,
                    grenseverdi = Prosent.`100_PROSENT`,
                    gradering = null
                )
            )
        )

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
                    grunnlag = Beløp("0"),
                    grunnlagsfaktor = GUnit(0),
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
                    grunnlag = Beløp("0"),
                    grunnlagsfaktor = GUnit(0),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 1,
                    barnetilleggsats = Beløp("36"),
                    barnetillegg = Beløp("36")
                )
            )
        )
    }

    @Test
    fun `minste årlige ytelse er lik 2G før 1 juli 2024 og lik 2,041G fom 1 juli 2024`() { //Denne må oppdateres når grunnbeløper endres 1. mai 2024
        val fødeselsdato = Fødselsdato(LocalDate.of(1985, 4, 1))
        val beregningsgrunnlag = Grunnlag11_19(
            GUnit(BigDecimal(0)),
            false,
            false
        )
        val underveisgrunnlag = UnderveisGrunnlag(
            id = 1L, listOf(
                Underveisperiode(
                    periode = Periode(LocalDate.of(2024, 6, 30), LocalDate.of(2024, 7, 1)),
                    utfall = Utfall.OPPFYLT,
                    avslagsårsak = null,
                    grenseverdi = Prosent.`100_PROSENT`,
                    gradering = null
                )
            )
        )

        val barnetilleggGrunnlag = BarnetilleggGrunnlag(1L, emptyList())

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(
            fødeselsdato,
            beregningsgrunnlag,
            underveisgrunnlag,
            barnetilleggGrunnlag
        ).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.of(2024, 6, 30), LocalDate.of(2024, 6, 30)),
                verdi = Tilkjent(
                    dagsats = Beløp("912.46"), //118620*2/260
                    gradering = Prosent.`0_PROSENT`,
                    grunnlag = Beløp("0"),
                    grunnlagsfaktor = GUnit(0),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("27"),
                    barnetillegg = Beløp("0")
                )
            ),
            Segment(
                periode = Periode(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 7, 1)),
                verdi = Tilkjent(
                    dagsats = Beløp("931.17"), //118620*2.041/260
                    gradering = Prosent.`0_PROSENT`,
                    grunnlag = Beløp("0"),
                    grunnlagsfaktor = GUnit(0),
                    grunnbeløp = Beløp("118620"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("27"),
                    barnetillegg = Beløp("0")
                )
            )
        )
    }

    @Test
    fun `Minste Årlig Ytelse justeres ift alder`() {
        val fødselsdato = Fødselsdato(LocalDate.of(1995, 4, 1))
        val beregningsgrunnlag = Grunnlag11_19(
            GUnit(BigDecimal(0)),
            false,
            false
        )
        val underveisgrunnlag = UnderveisGrunnlag(
            id = 1L, listOf(
                Underveisperiode(
                    periode = Periode(LocalDate.of(2020, 3, 31), LocalDate.of(2020, 4, 1)),
                    utfall = Utfall.OPPFYLT,
                    avslagsårsak = null,
                    grenseverdi = Prosent.`100_PROSENT`,
                    gradering = null
                )
            )
        )
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
                    grunnlag = Beløp("0"),
                    grunnlagsfaktor = GUnit(0),
                    grunnbeløp = Beløp("99858"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("27"),
                    barnetillegg = Beløp("0")
                )
            ),
            Segment(
                periode = Periode(LocalDate.of(2020, 4, 1), LocalDate.of(2020, 4, 1)),
                verdi = Tilkjent(
                    dagsats = Beløp("768.14"), //2*99858/260
                    gradering = Prosent.`0_PROSENT`,
                    grunnlag = Beløp("0"),
                    grunnlagsfaktor = GUnit(0),
                    grunnbeløp = Beløp("99858"),
                    antallBarn = 0,
                    barnetilleggsats = Beløp("27"),
                    barnetillegg = Beløp("0")
                )
            )
        )
    }
}
