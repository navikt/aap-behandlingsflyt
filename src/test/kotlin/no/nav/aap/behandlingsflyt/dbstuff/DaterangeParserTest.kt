package no.nav.aap.behandlingsflyt.dbstuff

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DaterangeParserTest {

    @Test
    fun `skal parse fra sql`() {
        val fom = LocalDate.now()
        val tom = LocalDate.now()
        val periode = DaterangeParser.fromSQL("[$fom,$tom]")

        assertThat(periode.fom).isEqualTo(fom)
        assertThat(periode.tom).isEqualTo(tom)
    }
}