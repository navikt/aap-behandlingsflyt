package no.nav.aap.behandlingsflyt.beregning.år

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Year

class InntektsBehovTest {

    @Test
    fun `skal utlede de tre forutgående kalenderårene fra nedsettelsesdato`() {
        val nedsettelsesDato = LocalDate.now().minusYears(3)
        val relevanteÅr = InntektsBehov(Input(nedsettelsesDato)).utledAlleRelevanteÅr()

        val nedsattYear = Year.of(nedsettelsesDato.year)

        assertThat(relevanteÅr).hasSize(3)
        assertThat(relevanteÅr).containsExactly(
            nedsattYear.minusYears(3),
            nedsattYear.minusYears(2),
            nedsattYear.minusYears(1)
        )
    }

    @Test
    fun `skal utlede de tre forutgående kalenderårene fra nedsettelsesdato og tre forutgående kalenderårene fra ytterligere nedsattdato`() {
        val nedsettelsesDato = LocalDate.now().minusYears(6)
        val ytterligereNedsattDato = LocalDate.now().minusYears(2)
        val relevanteÅr = InntektsBehov(Input(nedsettelsesDato, ytterligereNedsattDato)).utledAlleRelevanteÅr()

        val nedsattYear = Year.of(nedsettelsesDato.year)
        val ytterligereNedsattYear = Year.of(ytterligereNedsattDato.year)

        assertThat(relevanteÅr).hasSize(6)
        assertThat(relevanteÅr).containsExactly(
            nedsattYear.minusYears(3),
            nedsattYear.minusYears(2),
            nedsattYear.minusYears(1),
            ytterligereNedsattYear.minusYears(3),
            ytterligereNedsattYear.minusYears(2),
            ytterligereNedsattYear.minusYears(1)
        )
    }
}