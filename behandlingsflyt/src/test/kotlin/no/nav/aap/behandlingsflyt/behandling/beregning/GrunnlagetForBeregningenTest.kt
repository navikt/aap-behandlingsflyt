package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Year

class GrunnlagetForBeregningenTest {

    @Test
    fun `Det må oppgis tre inntekter for sammenhengende år, uten overlapp på år`() {
        val inntekterForToÅr = setOf(
            InntektPerÅr(
                Year.of(2022),
                Beløp(0)
            ),
            InntektPerÅr(Year.of(2021), Beløp(0))
        )
        val toÅrException = assertThrows<IllegalArgumentException> {
            GrunnlagetForBeregningen(inntekterForToÅr)
        }
        assertThat(toÅrException).hasMessage("Må oppgi tre inntekter")

        val inntekterForTreIkkesammenhengendeÅr = setOf(
            InntektPerÅr(
                Year.of(2022),
                Beløp(0)
            ),
            InntektPerÅr(
                Year.of(2021),
                Beløp(0)
            ),
            InntektPerÅr(Year.of(2019), Beløp(0))
        )
        val treIkkesammenhengendeÅrException = assertThrows<IllegalArgumentException> {
            GrunnlagetForBeregningen(inntekterForTreIkkesammenhengendeÅr)
        }
        assertThat(treIkkesammenhengendeÅrException).hasMessage("Inntektene må representere tre sammenhengende år")
    }

    @Test
    fun `Hvis bruker ikke har inntekt beregnes grunnlaget til 0 kr`() {
        val inntekterPerÅr = setOf(
            InntektPerÅr(Year.of(2022), Beløp(0)),
            InntektPerÅr(Year.of(2021), Beløp(0)),
            InntektPerÅr(Year.of(2020), Beløp(0))
        )
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntekterPerÅr)

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(
            Grunnlag11_19(
                grunnlaget = GUnit(BigDecimal(0)),
                erGjennomsnitt = false,
                gjennomsnittligInntektIG = GUnit(0),
                inntekter = listOf(
                    GrunnlagInntekt(
                        år = Year.of(2022),
                        inntektIKroner = Beløp(0),
                       grunnbeløp = Beløp(109_784),
                        inntektIG = GUnit(0),
                        inntekt6GBegrenset = GUnit(0),
                        er6GBegrenset = false
                    ),
                    GrunnlagInntekt(
                        år = Year.of(2021),
                        inntektIKroner = Beløp(0),
                       grunnbeløp = Beløp(104_716),
                        inntektIG = GUnit(0),
                        inntekt6GBegrenset = GUnit(0),
                        er6GBegrenset = false
                    ),
                    GrunnlagInntekt(
                        år = Year.of(2020),
                        inntektIKroner = Beløp(0),
                        grunnbeløp = Beløp(100_853),
                        inntektIG = GUnit(0),
                        inntekt6GBegrenset = GUnit(0),
                        er6GBegrenset = false
                    )
                )
            )
        )
    }

    @Test
    fun `Hvis bruker kun har inntekt siste kalenderår beregnes grunnlaget til inntekten dette året`() {
        val inntekterPerÅr = setOf(
            InntektPerÅr(
                Year.of(2022),
                Beløp(5 * 109_784) // 548 920
            ),
            InntektPerÅr(Year.of(2021), Beløp(0)),
            InntektPerÅr(Year.of(2020), Beløp(0))
        )
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntekterPerÅr)

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(
            Grunnlag11_19(
                grunnlaget = GUnit(BigDecimal(5)),
                erGjennomsnitt = false,
                gjennomsnittligInntektIG = GUnit("1.6666666667"),
                inntekter = listOf(
                    GrunnlagInntekt(
                        år = Year.of(2022),
                        inntektIKroner = Beløp(5 * 109_784), // 548 920
                        grunnbeløp = Beløp(109_784),
                        inntektIG = GUnit(5),
                        inntekt6GBegrenset = GUnit(5),
                        er6GBegrenset = false
                    ),
                    GrunnlagInntekt(
                        år = Year.of(2021),
                        inntektIKroner = Beløp(0),
                        grunnbeløp = Beløp(104_716),
                        inntektIG = GUnit(0),
                        inntekt6GBegrenset = GUnit(0),
                        er6GBegrenset = false
                    ),
                    GrunnlagInntekt(
                        år = Year.of(2020),
                        inntektIKroner = Beløp(0),
                        grunnbeløp = Beløp(100_853),
                        inntektIG = GUnit(0),
                        inntekt6GBegrenset = GUnit(0),
                        er6GBegrenset = false
                    )
                )
            )
        )
    }

    @Test
    fun `Hvis bruker har vesentlig høyere inntekt i kroner siste kalenderår beregnes grunnlaget til inntekten det siste året`() {
        val inntekterPerÅr = setOf(
            InntektPerÅr(
                Year.of(2022),
                Beløp(5 * 109_784) // 548 920
            ),
            InntektPerÅr(
                Year.of(2021),
                Beløp(2 * 104_716) // 209 432
            ),
            InntektPerÅr(
                Year.of(2020),
                Beløp(2 * 100_853) // 201 706
            )
        )
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntekterPerÅr)

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(
            Grunnlag11_19(
                grunnlaget = GUnit(BigDecimal(5)),
                erGjennomsnitt = false,
                gjennomsnittligInntektIG = GUnit(3),
                inntekter = listOf(
                    GrunnlagInntekt(
                        år = Year.of(2022),
                        inntektIKroner = Beløp(5 * 109_784), // 548 920
                        grunnbeløp = Beløp(109_784),
                        inntektIG = GUnit(5),
                        inntekt6GBegrenset = GUnit(5),
                        er6GBegrenset = false
                    ),
                    GrunnlagInntekt(
                        år = Year.of(2021),
                        inntektIKroner = Beløp(2 * 104_716), // 209 432
                        grunnbeløp = Beløp(104_716),
                        inntektIG = GUnit(2),
                        inntekt6GBegrenset = GUnit(2),
                        er6GBegrenset = false
                    ),
                    GrunnlagInntekt(
                        år = Year.of(2020),
                        inntektIKroner = Beløp(2 * 100_853), // 201 706
                        grunnbeløp = Beløp(100_853),
                        inntektIG = GUnit(2),
                        inntekt6GBegrenset = GUnit(2),
                        er6GBegrenset = false
                    )
                )

            )
        )
    }

    @Test
    fun `Hvis bruker har samme inntekt i G siste tre kalenderår brukes siste år som grunnlag`() {
        val inntekterPerÅr = setOf(
            InntektPerÅr(
                Year.of(2022),
                Beløp(5 * 109_784) // 548 920
            ),
            InntektPerÅr(
                Year.of(2021),
                Beløp(5 * 104_716) // 523 580
            ),
            InntektPerÅr(
                Year.of(2020),
                Beløp(5 * 100_853) // 504 265
            )
        )
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntekterPerÅr)

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(
            Grunnlag11_19(
                grunnlaget = GUnit(BigDecimal(5)),  // gjennomsnitt er også 5
                erGjennomsnitt = false,
                gjennomsnittligInntektIG = GUnit(5),
                inntekter = listOf(
                    GrunnlagInntekt(
                        år = Year.of(2022),
                        inntektIKroner = Beløp(5 * 109_784), // 548 920
                        grunnbeløp = Beløp(109_784),
                        inntektIG = GUnit(5),
                        inntekt6GBegrenset = GUnit(5),
                        er6GBegrenset = false
                    ),
                    GrunnlagInntekt(
                        år = Year.of(2021),
                        inntektIKroner = Beløp(5 * 104_716), // 523 580
                        grunnbeløp = Beløp(104_716),
                        inntektIG = GUnit(5),
                        inntekt6GBegrenset = GUnit(5),
                        er6GBegrenset = false
                    ),
                    GrunnlagInntekt(
                        år = Year.of(2020),
                        inntektIKroner = Beløp(5 * 100_853), // 504 265
                        grunnbeløp = Beløp(100_853),
                        inntektIG = GUnit(5),
                        inntekt6GBegrenset = GUnit(5),
                        er6GBegrenset = false
                    )
                )
            )
        )
    }

    @Test
    fun `Siste årets inntekt begrenses oppad til 6G`() {
        val inntekterPerÅr = setOf(
            InntektPerÅr(
                Year.of(2022),
                Beløp(7 * 109_784) // 768 488
            ),
            InntektPerÅr(
                Year.of(2021),
                Beløp(2 * 104_716) // 209 432
            ),
            InntektPerÅr(
                Year.of(2020),
                Beløp(2 * 100_853) // 201 706
            )
        )
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntekterPerÅr)

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(
            Grunnlag11_19(
                grunnlaget = GUnit(6),
                erGjennomsnitt = false,
                gjennomsnittligInntektIG = GUnit("3.3333333333"),
                inntekter = listOf(
                    GrunnlagInntekt(
                        år = Year.of(2022),
                        inntektIKroner = Beløp(7 * 109_784), // 768 488
                        grunnbeløp = Beløp(109_784),
                        inntektIG = GUnit(7),
                        inntekt6GBegrenset = GUnit(6),
                        er6GBegrenset = true
                    ),
                    GrunnlagInntekt(
                        år = Year.of(2021),
                        inntektIKroner = Beløp(2 * 104_716), // 209 432
                        grunnbeløp = Beløp(104_716),
                        inntektIG = GUnit(2),
                        inntekt6GBegrenset = GUnit(2),
                        er6GBegrenset = false
                    ),
                    GrunnlagInntekt(
                        år = Year.of(2020),
                        inntektIKroner = Beløp(2 * 100_853), // 201 706
                        grunnbeløp = Beløp(100_853),
                        inntektIG = GUnit(2),
                        inntekt6GBegrenset = GUnit(2),
                        er6GBegrenset = false
                    )
                )
            )
        )
    }

    @Test
    fun `Gjennomsnittlig inntekt siste tre år begrenses oppad til 6G`() {
        val inntekterPerÅr = setOf(
            InntektPerÅr(
                Year.of(2022),
                Beløp(7 * 109_784) // 768 488
            ),
            InntektPerÅr(
                Year.of(2021),
                Beløp(7 * 104_716) // 733 012
            ),
            InntektPerÅr(
                Year.of(2020),
                Beløp(7 * 100_853) // 705 971
            )
        )
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntekterPerÅr)

        val beregnetGrunnlag = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(beregnetGrunnlag.grunnlaget()).isEqualTo(GUnit(6))
        assertThat(beregnetGrunnlag).isEqualTo(
            Grunnlag11_19(
                grunnlaget = GUnit(6),
                erGjennomsnitt = false,
                gjennomsnittligInntektIG = GUnit(6),
                inntekter = listOf(
                    GrunnlagInntekt(
                        år = Year.of(2022),
                        inntektIKroner = Beløp(7 * 109_784), // 768 488
                        grunnbeløp = Beløp(109_784),
                        inntektIG = GUnit(7),
                        inntekt6GBegrenset = GUnit(6),
                        er6GBegrenset = true
                    ),
                    GrunnlagInntekt(
                        år = Year.of(2021),
                        inntektIKroner = Beløp(7 * 104_716), // 733 012
                        grunnbeløp = Beløp(104_716),
                        inntektIG = GUnit(7),
                        inntekt6GBegrenset = GUnit(6),
                        er6GBegrenset = true
                    ),
                    GrunnlagInntekt(
                        år = Year.of(2020),
                        inntektIKroner = Beløp(7 * 100_853), // 705 971
                        grunnbeløp = Beløp(100_853),
                        inntektIG = GUnit(7),
                        inntekt6GBegrenset = GUnit(6),
                        er6GBegrenset = true
                    )
                )
            )
        )
    }

    @Test
    fun `om nyeste inntekt er lav, så brukes gjennomsnitt`() {
        val inntekterPerÅr = setOf(
            InntektPerÅr(
                Year.of(2022),
                Beløp(3 * 109_784) // 329 352
            ),
            InntektPerÅr(
                Year.of(2021),
                Beløp(4 * 104_716) // 418 864
            ),
            InntektPerÅr(
                Year.of(2020),
                Beløp(5 * 100_853) // 504 265
            )
        )

        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntekterPerÅr)

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(
            Grunnlag11_19(
                grunnlaget = GUnit(4),
                erGjennomsnitt = true,
                gjennomsnittligInntektIG = GUnit(4),
                inntekter = listOf(
                    GrunnlagInntekt(
                        år = Year.of(2022),
                        inntektIKroner = Beløp(3 * 109_784), // 329 352
                        grunnbeløp = Beløp(109_784),
                        inntektIG = GUnit(3),
                        inntekt6GBegrenset = GUnit(3),
                        er6GBegrenset = false
                    ),
                    GrunnlagInntekt(
                        år = Year.of(2021),
                        inntektIKroner = Beløp(4 * 104_716), // 418 864
                        grunnbeløp = Beløp(104_716),
                        inntektIG = GUnit(4),
                        inntekt6GBegrenset = GUnit(4),
                        er6GBegrenset = false
                    ),
                    GrunnlagInntekt(
                        år = Year.of(2020),
                        inntektIKroner = Beløp(5 * 100_853), // 504 265
                        grunnbeløp = Beløp(100_853),
                        inntektIG = GUnit(5),
                        inntekt6GBegrenset = GUnit(5),
                        er6GBegrenset = false
                    )
                )
            )
        )
    }

    @Test
    fun `Hvert av kalenderårene begrenses individuelt oppad til 6G før gjennomsnittet beregnes`() {
        val inntekterPerÅr = setOf(
            InntektPerÅr(
                Year.of(2022),
                Beløp(3 * 109_784) //   329 352
            ),
            InntektPerÅr(
                Year.of(2021),
                Beløp(3 * 104_716) //   314 148
            ),
            InntektPerÅr(
                Year.of(2020),
                Beløp(12 * 100_853) // 1 210 236
            )
        )
        val grunnlagetForBeregningen = GrunnlagetForBeregningen(inntekterPerÅr)

        val grunnlaget = grunnlagetForBeregningen.beregnGrunnlaget()

        assertThat(grunnlaget).isEqualTo(
            Grunnlag11_19(
                grunnlaget = GUnit(4),
                erGjennomsnitt = true,
                gjennomsnittligInntektIG = GUnit(4),
                inntekter = listOf(
                    GrunnlagInntekt(
                        år = Year.of(2022),
                        inntektIKroner = Beløp(3 * 109_784), //   329 352
                        grunnbeløp = Beløp(109_784),
                        inntektIG = GUnit(3),
                        inntekt6GBegrenset = GUnit(3),
                        er6GBegrenset = false
                    ),
                    GrunnlagInntekt(
                        år = Year.of(2021),
                        inntektIKroner = Beløp(3 * 104_716), //   314 148
                        grunnbeløp = Beløp(104_716),
                        inntektIG = GUnit(3),
                        inntekt6GBegrenset = GUnit(3),
                        er6GBegrenset = false
                    ),
                    GrunnlagInntekt(
                        år = Year.of(2020),
                        inntektIKroner = Beløp(12 * 100_853), // 1 210 236
                        grunnbeløp = Beløp(100_853),
                        inntektIG = GUnit(12),
                        inntekt6GBegrenset = GUnit(6),
                        er6GBegrenset = true
                    )
                )
            )
        )
    }
}
