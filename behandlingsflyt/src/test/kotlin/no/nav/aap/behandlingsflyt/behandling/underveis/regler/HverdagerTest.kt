package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plusHverdager
import no.nav.aap.behandlingsflyt.test.november
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HverdagerTest {
    @Test
    fun `teller hverdager i periode`() {
        val periode = Periode(10 november 2025, 16 november 2025)

        val hverdager = periode.antallHverdager()

        assertThat(hverdager.asInt).isEqualTo(5)
    }

    @Test
    fun `legge til hverdager på LocalDate`() {
        // Lørdag
        val dato = 15 november 2025

        // Første hverdag er mandag 18.
        assertThat(dato.plusHverdager(Hverdager(1))).isEqualTo(18 november 2025)
    }
}