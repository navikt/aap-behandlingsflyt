package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.Beløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.GUnit
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.adapter.InntektPerÅr
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class YrkesskadeBeregningTest {

    private companion object {
        private const val GRUNNBELØP_2022 = 109784
    }

    @Test
    fun `Hvis ingen yrkesskadeandel, så returneres samme grunnlag som 11-19`() {
        val grunnlag11_19 = Grunnlag11_19(GUnit(4))

        val yrkesskadeBeregning = YrkesskadeBeregning(
            grunnlag11_19 = grunnlag11_19,
            antattÅrligInntekt = InntektPerÅr(2022, Beløp(0)),
            andelAvNedsettelsenSomSkyldesYrkesskaden = Prosent(0)
        )

        val grunnlag = yrkesskadeBeregning.beregnYrkesskaden()

        assertThat(grunnlag.grunnlaget()).isEqualTo(GUnit(4))
    }

    @Test
    fun `Hvis antatt årlig arbeidsinntekt er lavere enn 11-19, så settes grunnlaget tilsvarende grunnlag fra 11-19 uavhengig av yrkesskadeandel`() {
        val grunnlag11_19 = Grunnlag11_19(GUnit(4))

        val yrkesskadeBeregning = YrkesskadeBeregning(
            grunnlag11_19 = grunnlag11_19,
            antattÅrligInntekt = InntektPerÅr(2022, Beløp(2 * GRUNNBELØP_2022)),   // 219 568
            andelAvNedsettelsenSomSkyldesYrkesskaden = Prosent(100)
        )

        val grunnlag = yrkesskadeBeregning.beregnYrkesskaden()

        assertThat(grunnlag.grunnlaget()).isEqualTo(GUnit(4))
    }

    @Test
    fun `Hvis yrkesskadeandel er 70 prosent, og antatt årlig arbeidsinntekt er høyere enn 11-19, beregnes grunnlaget med 30 prosent 11-19 og 70 prosent yrkesskade`() {
        val grunnlag11_19 = Grunnlag11_19(GUnit(2))

        val yrkesskadeBeregning = YrkesskadeBeregning(
            grunnlag11_19 = grunnlag11_19,
            antattÅrligInntekt = InntektPerÅr(2022, Beløp(4 * GRUNNBELØP_2022)), // 439 136
            andelAvNedsettelsenSomSkyldesYrkesskaden = Prosent(70)
        )

        val grunnlag = yrkesskadeBeregning.beregnYrkesskaden()

        assertThat(grunnlag.grunnlaget()).isEqualTo(GUnit("3.4"))
    }

    @Test
    fun `Hvis yrkesskadeandel er 71 prosent, og antatt årlig arbeidsinntekt er høyere enn 11-19, beregnes grunnlaget med 0 prosent 11-19 og 100 prosent yrkesskade`() {
        val grunnlag11_19 = Grunnlag11_19(GUnit(2))

        val yrkesskadeBeregning = YrkesskadeBeregning(
            grunnlag11_19 = grunnlag11_19,
            antattÅrligInntekt = InntektPerÅr(2022, Beløp(4 * GRUNNBELØP_2022)), // 439 136
            andelAvNedsettelsenSomSkyldesYrkesskaden = Prosent(71)
        )

        val grunnlag = yrkesskadeBeregning.beregnYrkesskaden()

        assertThat(grunnlag.grunnlaget()).isEqualTo(GUnit(4))
    }

    @Test
    fun `Hvis yrkesskadeandel er 100 prosent, og antatt årlig arbeidsinntekt er høyere enn 11-19, så settes grunnlaget tilsvarende antatt årlig arbeidsinntekt`() {
        val grunnlag11_19 = Grunnlag11_19(GUnit(2))

        val yrkesskadeBeregning = YrkesskadeBeregning(
            grunnlag11_19 = grunnlag11_19,
            antattÅrligInntekt = InntektPerÅr(2022, Beløp(4 * GRUNNBELØP_2022)),   // 439 136
            andelAvNedsettelsenSomSkyldesYrkesskaden = Prosent(100)
        )

        val grunnlag = yrkesskadeBeregning.beregnYrkesskaden()

        assertThat(grunnlag.grunnlaget()).isEqualTo(GUnit(4))
    }
}
