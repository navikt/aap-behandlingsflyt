package no.nav.aap.behandlingsflyt.tilkjentytelse

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.Tilkjent
import no.nav.aap.tidslinje.Segment
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import java.math.BigDecimal
import java.time.LocalDate

class BeregnTilkjentYtelseServiceTest {



    @Test
    fun `årlig ytelse beregnes til 66 prosent av grunnlaget og dagsatsen er lik årlig ytelse delt på 260`() {
        val beregningsgrunnlag = Grunnlag11_19(
            GUnit(BigDecimal(4))
        )
        val underveisgrunnlag = UnderveisGrunnlag(
            id=1L, listOf(
                Underveisperiode(
                    periode = Periode(LocalDate.of(2024,2,8), LocalDate.of(2024,2,8)),
                    utfall = Utfall.OPPFYLT,
                    avslagsårsak = null,
                    grenseverdi = Prosent.`100_PROSENT`,
                    gradering = null
                )
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(beregningsgrunnlag, underveisgrunnlag).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.of(2024,2,8), LocalDate.of(2024,2,8)),
                verdi = Tilkjent(
                    dagsats = Beløp("1204.45"), //4*0.66*118620/260
                    gradering = Prosent.`0_PROSENT`
                )
            )
        )
    }

    @Test
    fun `minste årlige ytelse er lik 2G før 1 juli 2024 og lik 2,041G fom 1 juli 2024`() { //Denne må oppdateres når grunnbeløper endres 1. mai 2024
        val beregningsgrunnlag = Grunnlag11_19(
            GUnit(BigDecimal(0))
        )
        val underveisgrunnlag = UnderveisGrunnlag(
            id=1L, listOf(
                Underveisperiode(
                    periode = Periode(LocalDate.of(2024,6,30), LocalDate.of(2024,7,1)),
                    utfall = Utfall.OPPFYLT,
                    avslagsårsak = null,
                    grenseverdi = Prosent.`100_PROSENT`,
                    gradering = null
                )
            )
        )

        val beregnTilkjentYtelseService = BeregnTilkjentYtelseService(beregningsgrunnlag, underveisgrunnlag).beregnTilkjentYtelse()

        assertThat(beregnTilkjentYtelseService.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.of(2024,6,30), LocalDate.of(2024,6,30)),
                verdi = Tilkjent(
                    dagsats = Beløp("912.46"), //118620*2/260
                    gradering = Prosent.`0_PROSENT`
                )
            ),
            Segment(
                periode = Periode(LocalDate.of(2024,7,1), LocalDate.of(2024,7,1)),
                verdi = Tilkjent(
                    dagsats = Beløp("931.17"), //118620*2.041/260
                    gradering = Prosent.`0_PROSENT`
                )
            )
        )
    }


}