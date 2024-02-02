package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagetForBeregningen
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Year

class GrunnlagetForBeregningenTest {

    @Test
    fun `Det må oppgis tre inntekter for sammenhengende år, uten overlapp på år`() {
        val inntekterForToÅr = setOf(
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2022),
                Beløp(BigDecimal(0))
            ),
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(Year.of(2021), Beløp(BigDecimal(0)))
        )
        val toÅrException = assertThrows<IllegalArgumentException> {
            GrunnlagetForBeregningen(inntekterForToÅr)
        }
        assertThat(toÅrException).hasMessage("Må oppgi tre inntekter")

        val inntekterForTreIkkesammenhengendeÅr = setOf(
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2022),
                Beløp(BigDecimal(0))
            ),
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2021),
                Beløp(BigDecimal(0))
            ),
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(Year.of(2019), Beløp(BigDecimal(0)))
        )
        val treIkkesammenhengendeÅrException = assertThrows<IllegalArgumentException> {
            GrunnlagetForBeregningen(inntekterForTreIkkesammenhengendeÅr)
        }
        assertThat(treIkkesammenhengendeÅrException).hasMessage("Inntektene må representere tre sammenhengende år")
    }

    @Test
    fun `Hvis bruker ikke har inntekt beregnes grunnlaget til 0 kr`() {
        val inntekterPerÅr = setOf(
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2022),
                Beløp(BigDecimal(0))
            ),
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2021),
                Beløp(BigDecimal(0))
            ),
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(Year.of(2020), Beløp(BigDecimal(0)))
        )
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntekterPerÅr)

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(
            Grunnlag11_19(
                GUnit(BigDecimal(0))
            )
        )
    }

    @Test
    fun `Hvis bruker kun har inntekt siste kalenderår beregnes grunnlaget til inntekten dette året`() {
        val inntekterPerÅr = setOf(
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2022),
                Beløp(BigDecimal(5 * 109_784))
            ),    // 548 920
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2021),
                Beløp(BigDecimal(0))
            ),
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(Year.of(2020), Beløp(BigDecimal(0)))
        )
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntekterPerÅr)

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(
            Grunnlag11_19(
                GUnit(BigDecimal(5))
            )
        )
    }

    @Test
    fun `Hvis bruker har vesentlig høyere inntekt i kroner siste kalenderår beregnes grunnlaget til inntekten det siste året`() {
        val inntekterPerÅr = setOf(
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2022),
                Beløp(BigDecimal(5 * 109_784))
            ),   // 548 920
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2021),
                Beløp(BigDecimal(2 * 104_716))
            ),   // 209 432
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2020),
                Beløp(BigDecimal(2 * 100_853))
            )    // 201 706
        )
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntekterPerÅr)

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(
            Grunnlag11_19(
                GUnit(BigDecimal(5))
            )
        )
    }

    @Test
    fun `Hvis bruker har samme inntekt i kroner siste tre kalenderår beregnes grunnlaget til gjennomsnittet av inntektene`() {
        val inntekterPerÅr = setOf(
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2022),
                Beløp(BigDecimal(5 * 109_784))
            ),   // 548 920
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2021),
                Beløp(BigDecimal(5 * 104_716))
            ),   // 523 580
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2020),
                Beløp(BigDecimal(5 * 100_853))
            )    // 504 265
        )
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntekterPerÅr)

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(
            Grunnlag11_19(
                GUnit(BigDecimal(5))
            )
        )
    }

    @Test
    fun `Siste årets inntekt begrenses oppad til 6G`() {
        val inntekterPerÅr = setOf(
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2022),
                Beløp(BigDecimal(7 * 109_784))
            ),   // 768 488
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2021),
                Beløp(BigDecimal(2 * 104_716))
            ),   // 209 432
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2020),
                Beløp(BigDecimal(2 * 100_853))
            )    // 201 706
        )
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntekterPerÅr)

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(
            Grunnlag11_19(
                GUnit(BigDecimal(6))
            )
        )
    }

    @Test
    fun `Gjennomsnittlig inntekt siste tre år begrenses oppad til 6G`() {
        val inntekterPerÅr = setOf(
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2022),
                Beløp(BigDecimal(7 * 109_784))
            ),   // 768 488
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2021),
                Beløp(BigDecimal(7 * 104_716))
            ),   // 733 012
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2020),
                Beløp(BigDecimal(7 * 100_853))
            )    // 705 971
        )
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntekterPerÅr)

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(
            Grunnlag11_19(
                GUnit(BigDecimal(6))
            )
        )
    }

    @Test
    fun `Hvert av kalenderårene begrenses individuelt oppad til 6G før gjennomsnittet beregnes`() {
        val inntekterPerÅr = setOf(
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2022),
                Beløp(BigDecimal(3 * 109_784))
            ),    //   329 352
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2021),
                Beløp(BigDecimal(3 * 104_716))
            ),    //   314 148
            no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                Year.of(2020),
                Beløp(BigDecimal(12 * 100_853))
            )    // 1 210 236
        )
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntekterPerÅr)

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(
            Grunnlag11_19(
                GUnit(BigDecimal(4))
            )
        )
    }
}
