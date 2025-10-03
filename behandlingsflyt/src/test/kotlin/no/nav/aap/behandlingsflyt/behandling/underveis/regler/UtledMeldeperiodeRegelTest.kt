package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UtledMeldeperiodeRegel.Companion.MELDEPERIODE_LENGDE
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtledMeldeperiodeRegelTest {

    /*
     * 2020
     *         July                     August                  September
     * Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su      Mo Tu We Th Fr Sa Su
     *        1  2  3  4  5                      1  2          1  2  3  4  5  6
     *  6  7  8  9 10 11 12       3  4  5  6  7  8  9       7  8  9 10 11 12 13
     * 13 14 15 16 17 18 19      10 11 12 13 14 15 16      14 15 16 17 18 19 20
     * 20 21 22 23 24 25 26      17 18 19 20 21 22 23      21 22 23 24 25 26 27
     * 27 28 29 30 31            24 25 26 27 28 29 30      28 29 30
     *                     31
     */
    @Test
    fun `går til tidligere mandag hvis rettighetsperiode ikke starter på mandag`() {
        val rettighetperiode = Periode(7 juli 2020, 3 august 2020)
        val input = tomUnderveisInput(rettighetsperiode = rettighetperiode)
        val resultat = UtledMeldeperiodeRegel().vurder(input, Tidslinje())

        assertTidslinje(
            resultat,
            Periode(7 juli 2020, 19 juli 2020) to {
                assertEquals(Periode(6 juli 2020, 19 juli 2020), it.meldeperiode())
            },
            Periode(20 juli 2020, 2 august 2020) to {
                assertEquals(Periode(20 juli 2020, 2 august 2020), it.meldeperiode())
            },
            Periode(3 august 2020, 3 august 2020) to {
                assertEquals(Periode(3 august 2020, 16 august 2020), it.meldeperiode())
            },
        )
    }

    /*
     *  2020
     *        April
     *  Mo Tu We Th Fr Sa Su
     *         1  2  3  4  5
     *   6  7  8  9 10 11 12
     *  13 14 15 16 17 18 19
     *  20 21 22 23 24 25 26
     *  27 28 29 30
     */
    @Test
    fun `utlede meldeperiode hvor det er flere vurderinger i samme meldeperiode`() {
        val rettighetperiode = Periode(6 april 2020, 19 april 2020)
        val input = tomUnderveisInput(rettighetsperiode = rettighetperiode)
        val segmenter = listOf(
            Segment(Periode(6 april 2020, 9 april 2020), Vurdering()),
            Segment(Periode(10 april 2020, 19 april 2020), Vurdering())
        )
        val resultat = UtledMeldeperiodeRegel().vurder(input, Tidslinje(segmenter))

        assertEquals(2, resultat.segmenter().count())
        assertEquals(
            Vurdering(meldeperiode = meldeperiode(6 april 2020)),
            resultat.segmenter().first().verdi
        )
        assertEquals(
            Vurdering(meldeperiode = meldeperiode(6 april 2020)),
            resultat.segmenter().last().verdi
        )
    }

    private fun meldeperiode(startDate: LocalDate): Periode {
        return Periode(startDate, startDate.plusDays(MELDEPERIODE_LENGDE - 1))
    }
}