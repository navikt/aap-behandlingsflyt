package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UtledMeldeperiodeRegel.Companion.MELDEPERIODE_LENGDE
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class UtledMeldeperiodeRegelTest {

    @Test
    fun `utlede meldeperiode for rettighetsperiode som er akkurat 2 meldeperioder lang`() {
        val rettighetperiode = Periode(LocalDate.now(), LocalDate.now().plusDays((MELDEPERIODE_LENGDE * 2) - 1))
        val input = tomUnderveisInput.copy(rettighetsperiode = rettighetperiode)
        val resultat = UtledMeldeperiodeRegel().vurder(input, Tidslinje())

        assertEquals(2, resultat.segmenter().size)
        assertEquals(
            Vurdering(meldeperiode = meldeperiode(rettighetperiode.fom)),
            resultat.segmenter().first.verdi
        )
        assertEquals(
            Vurdering(meldeperiode = meldeperiode(rettighetperiode.fom.plusDays(14))),
            resultat.segmenter().last.verdi
        )
    }

    @Test
    fun `utlede meldeperiode hvor det er flere vurderinger i samme meldeperiode`() {
        val rettighetperiode = Periode(LocalDate.now(), LocalDate.now().plusDays(MELDEPERIODE_LENGDE - 1))
        val sluttFørsteRettighetsperiode = rettighetperiode.fom.plusDays(3)
        val input = tomUnderveisInput.copy(rettighetsperiode = rettighetperiode)
        val segmenter = listOf(
            Segment(Periode(rettighetperiode.fom, sluttFørsteRettighetsperiode), Vurdering()),
            Segment(Periode(sluttFørsteRettighetsperiode.plusDays(1), rettighetperiode.tom), Vurdering())
        )
        val resultat = UtledMeldeperiodeRegel().vurder(input, Tidslinje(segmenter))

        assertEquals(2, resultat.segmenter().size)
        assertEquals(
            Vurdering(meldeperiode = meldeperiode(rettighetperiode.fom)),
            resultat.segmenter().first.verdi
        )
        assertEquals(
            Vurdering(meldeperiode = meldeperiode(rettighetperiode.fom)),
            resultat.segmenter().last.verdi
        )
    }

    private fun meldeperiode(startDate: LocalDate): Periode {
        return Periode(startDate, startDate.plusDays(MELDEPERIODE_LENGDE - 1))
    }
}