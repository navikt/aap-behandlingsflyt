package no.nav.aap.behandlingsflyt.dbconnect

import no.nav.aap.verdityper.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal class DaterangeParserTest {

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

    @Test
    fun `Parser daterange der sluttdato er uendelig frem i tid`() {
        val fom = LocalDate.now()
        val tom = DaterangeParser.MAX_DATE.format(DateTimeFormatter.ofPattern("u-MM-dd"))
        val periode = DaterangeParser.fromSQL("[$fom,$tom)")

        assertThat(periode.fom).isEqualTo(fom)
        assertThat(periode.tom).isEqualTo(LocalDate.MAX)
    }

    @Test
    fun `Parser daterange der startdato er uendelig bakover i tid`() {
        val tom = LocalDate.now()
        val fom = DaterangeParser.MIN_DATE
        val periode = DaterangeParser.fromSQL("[$fom,$tom]")

        assertThat(periode.fom).isEqualTo(LocalDate.MIN)
        assertThat(periode.tom).isEqualTo(tom)
    }
}
