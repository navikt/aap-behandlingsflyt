package no.nav.aap.domene.typer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class PeriodeTest {

    @Test
    fun `teste validering - ingen feil`() {
        Periode(LocalDate.MIN, LocalDate.MAX)
        Periode(LocalDate.now(), LocalDate.now())
        Periode(LocalDate.now().minusDays(1), LocalDate.now())
    }

    @Test
    fun `teste validering - tom før fom`() {
        assertThrows<IllegalArgumentException> {
            Periode(LocalDate.MAX, LocalDate.MIN)
        }
    }

    @Test
    fun `teste overlapp - overlapper`() {
        val periode = Periode(LocalDate.now().minusDays(14), LocalDate.now().minusDays(7))
        val periode2 = Periode(LocalDate.now().minusDays(8), LocalDate.now())

        assert(periode.overlapper(periode2))

        val periode1 = Periode(LocalDate.now(), LocalDate.now())
        val periode3 = Periode(LocalDate.now(), LocalDate.now())

        assert(periode1.overlapper(periode3))
    }

    @Test
    fun `teste overlapp - overlapper ikke`() {
        val periode = Periode(LocalDate.now().minusDays(14), LocalDate.now().minusDays(7))
        val periode2 = Periode(LocalDate.now().minusDays(6), LocalDate.now())

        assert(!periode.overlapper(periode2))
    }
}
