package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.komponenter.verdityper.Beløp
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Year
import java.time.YearMonth

class InntektValideringTest {

    @Test
    fun `skal feile når forskjellen mellom inntekt fra A-inntekt og POPP er mer enn 100kr`() {
        val år = Year.of(2022)
        val årsInntekter = setOf(InntektPerÅr(år, Beløp(500000)))
        val månedsinntekter = mapOf(YearMonth.of(2022, 1) to Beløp(500102))

        assertThrows<IllegalArgumentException> {
            InntektValidering.validerSummertInntekt(år, månedsinntekter, årsInntekter)
        }
    }

    @Test
    fun `skal ikke feile når forskjellen er under 100kr`() {
        val år = Year.of(2022)
        val årsInntekter = setOf(InntektPerÅr(år, Beløp(500000)))
        val månedsinntekter = mapOf(YearMonth.of(2022, 1) to Beløp(BigDecimal(499999.5)))

        assertDoesNotThrow {
            InntektValidering.validerSummertInntekt(år, månedsinntekter, årsInntekter)
        }
    }
}
