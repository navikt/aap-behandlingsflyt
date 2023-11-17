package no.nav.aap.behandlingsflyt.dbconnect

import no.nav.aap.behandlingsflyt.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DaterangeParserTest {

    @Test
    fun `Konverterer Periode til lukket daterange`() {
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusDays(10)
        val periode = DaterangeParser.toSQL(Periode(fom, tom))

        assertThat(periode).isEqualTo("[$fom,$tom]")
    }

    @Test
    fun `Parser daterange der både fom og tom er lukket`() {
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusDays(10)
        val periode = DaterangeParser.fromSQL("[$fom,$tom]")

        assertThat(periode.fom).isEqualTo(fom)
        assertThat(periode.tom).isEqualTo(tom)
    }

    @Test
    fun `Parser daterange der fom er lukket og tom er åpen`() {
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusDays(10)
        val periode = DaterangeParser.fromSQL("[$fom,$tom)")

        assertThat(periode.fom).isEqualTo(fom)
        assertThat(periode.tom.plusDays(1)).isEqualTo(tom)
    }

    @Test
    fun `Parser daterange der fom er åpen og tom er lukket`() {
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusDays(10)
        val periode = DaterangeParser.fromSQL("($fom,$tom]")

        assertThat(periode.fom.minusDays(1)).isEqualTo(fom)
        assertThat(periode.tom).isEqualTo(tom)
    }

    @Test
    fun `Parser daterange der både fom og tom er åpne`() {
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusDays(10)
        val periode = DaterangeParser.fromSQL("($fom,$tom)")

        assertThat(periode.fom.minusDays(1)).isEqualTo(fom)
        assertThat(periode.tom.plusDays(1)).isEqualTo(tom)
    }
}