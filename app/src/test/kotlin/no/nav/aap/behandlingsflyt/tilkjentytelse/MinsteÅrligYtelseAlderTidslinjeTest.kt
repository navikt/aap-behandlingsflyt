package no.nav.aap.behandlingsflyt.tilkjentytelse

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.MinsteÅrligYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import java.time.LocalDate

class MinsteÅrligYtelseAlderTidslinjeTest {

    @Test
    fun `produserer korrekt tidlinje ut fra fødselsdato`() {
        val fødelsdato = Fødselsdato(LocalDate.of(1990, 1, 2))
        val tidslinje = MinsteÅrligYtelseAlderTidslinje(fødelsdato).tilTidslinje()

        assertThat(tidslinje.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.MIN, LocalDate.of(2015, 1, 1)),
                verdi = AlderStrategi.Under25
            ),
            Segment(
                periode = Periode(LocalDate.of(2015, 1, 2), LocalDate.MAX),
                verdi = AlderStrategi.Over25
            ),
        )
    }

    @Test
    fun `blir riktig med skuddårs fødsel`() {
        val fødelsdato = Fødselsdato(LocalDate.of(1996, 2, 29))
        val tidslinje = MinsteÅrligYtelseAlderTidslinje(fødelsdato).tilTidslinje()

        assertThat(tidslinje.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.MIN, LocalDate.of(2021, 2, 27)),
                verdi = AlderStrategi.Under25
            ),
            Segment(
                periode = Periode(LocalDate.of(2021, 2, 28), LocalDate.MAX),
                verdi = AlderStrategi.Over25
            ),
        )
    }

    @Test
    fun `riktig minste ytelse utregning`() {
        val fødelsdato = Fødselsdato(LocalDate.of(1996, 2, 29))
        val minsteÅrligYtelseAlderTidslinje = MinsteÅrligYtelseAlderTidslinje(fødelsdato).tilTidslinje()

        val minsteÅrligYtelseTidslinje = MinsteÅrligYtelse.tilTidslinje()

        val tidslinje = minsteÅrligYtelseAlderTidslinje.kombiner(
            minsteÅrligYtelseTidslinje,
            JoinStyle.INNER_JOIN
        ) { periode, venstre:Segment<AlderStrategi>?, høyre: Segment<GUnit>? ->
            val minsteÅrligYtelse = requireNotNull(høyre?.verdi)
            val aldersfunksjon = requireNotNull(venstre?.verdi)
            Segment(periode, aldersfunksjon.aldersjustering(minsteÅrligYtelse))
        }


        assertThat(tidslinje.segmenter()).containsExactly(
            Segment(
                periode = Periode(LocalDate.MIN, LocalDate.of(2021, 2, 27)),
                verdi = GUnit(2).multiplisert(2).dividert(3)
            ),
            Segment(
                periode = Periode(LocalDate.of(2021, 2, 28), LocalDate.of(2024,6,30)),
                verdi = GUnit(2)
            ),
            Segment(
                periode = Periode(LocalDate.of(2024,7,1), LocalDate.MAX),
                verdi = GUnit("2.041")
            )
        )
    }
}