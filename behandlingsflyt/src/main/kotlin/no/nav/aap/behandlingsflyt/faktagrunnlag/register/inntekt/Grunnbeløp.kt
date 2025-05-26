package no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt

import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import java.time.LocalDate
import java.time.Year

object Grunnbeløp {
    private val grunnbeløpene = sortedSetOf<Element>()
    private val gjennomsnittsbeløpene = sortedSetOf<GjennomsnittElement>()

    init {
        element(2025, 5, 130_160, 128_116)
        element(2024, 5, 124_028, 122_225)
        element(2023, 5, 118_620, 116_239)
        element(2022, 5, 111_477, 109_784)
        element(2021, 5, 106_399, 104_716)
        element(2020, 5, 101_351, 100_853)
        element(2019, 5, 99_858, 98_866)
        element(2018, 5, 96_883, 95_800)
        element(2017, 5, 93_634, 93_281)
        element(2016, 5, 92_576, 91_740)
        element(2015, 5, 90_068, 89_502)
        element(2014, 5, 88_370, 87_328)
        element(2013, 5, 85_245, 84_204)
        element(2012, 5, 82_122, 81_153)
        element(2011, 5, 79_216, 78_024)
        element(2010, 5, 75_641, 74_721)
        element(2009, 5, 72_881, 72_006)
        element(2008, 5, 70_256, 69_108)
        element(2007, 5, 66_812, 65_505)
        element(2006, 5, 62_892, 62_161)
        element(2005, 5, 60_699, 60_059)
        element(2004, 5, 58_778, 58_139)
        element(2003, 5, 56_861, 55_964)
        element(2002, 5, 54_170, 53_233)
        element(2001, 5, 51_360, 50_603)
        element(2000, 5, 49_090, 48_377)
        element(1999, 5, 46_950, 46_423)
        element(1998, 5, 45_370, 44_413)
        element(1997, 5, 42_500, 42_000)
        element(1996, 5, 41_000, 40_410)
        element(1995, 5, 39_230, 38_847)
        element(1994, 5, 38_080, 37_820)
        element(1993, 5, 37_300, 37_033)
        element(1992, 5, 36_500, 36_167)
        element(1991, 5, 35_500, 35_033)
        element(1990, 12, 34_100, 33_575)
        element(1990, 5, 34_000)
        element(1989, 4, 32_700, 32_275)
        element(1988, 4, 31_000, 30_850)
        element(1988, 1, 30_400)
        element(1987, 5, 29_900, 29_267)
        element(1986, 5, 28_000, 27_433)
        element(1986, 1, 26_300)
        element(1985, 5, 25_900, 25_333)
        element(1984, 5, 24_200, 23_667)
        element(1983, 5, 22_600, 22_333)
        element(1983, 1, 21_800)
        element(1982, 5, 21_200, 20_667)
        element(1981, 10, 19_600, 18_658)
        element(1981, 5, 19_100)
        element(1981, 1, 17_400)
        element(1980, 5, 16_900, 16_633)
        element(1980, 1, 16_100)
        element(1979, 1, 15_200, 15_200)
        element(1978, 7, 14_700, 14_550)
        element(1977, 12, 14_400, 13_383)
        element(1977, 5, 13_400)
        element(1977, 1, 13_100)
        element(1976, 5, 12_100, 12_000)
        element(1976, 1, 11_800)
        element(1975, 5, 11_000, 10_800)
        element(1975, 1, 10_400)
        element(1974, 5, 9_700, 9_533)
        element(1974, 1, 9_200)
        element(1973, 1, 8_500, 8_500)
        element(1972, 1, 7_900, 7_900)
        element(1971, 5, 7_500, 7_400)
        element(1971, 1, 7_200)
        element(1970, 1, 6_800, 6_800)
        element(1969, 1, 6_400, 6_400)
        element(1968, 1, 5_900, 5_900)
        element(1967, 1, 5_400, 5_400)
    }

    private fun element(år: Int, måned: Int, beløp: Int) {
        val element = Element(år, måned, beløp)
        if (!grunnbeløpene.add(element)) {
            error("Må være unike grunnbeløp ($element)")
        }
    }

    private fun element(år: Int, måned: Int, beløp: Int, gjennomsnittBeløp: Int) {
        element(år, måned, beløp)
        val gjennomsnittElement = GjennomsnittElement(år, gjennomsnittBeløp)
        if (!gjennomsnittsbeløpene.add(gjennomsnittElement)) {
            error("Må være unike gjennomsnittsbeløp ($gjennomsnittElement)")
        }
    }

    private class Element private constructor(
        private val dato: LocalDate,
        private val beløp: Beløp
    ) : Comparable<Element> {

        constructor(år: Int, måned: Int, beløp: Int) : this(
            dato = LocalDate.of(år, måned, 1),
            beløp = Beløp(beløp)
        )

        override fun compareTo(other: Element): Int {
            return this.dato.compareTo(other.dato)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Element

            return dato == other.dato
        }

        override fun hashCode(): Int {
            return dato.hashCode()
        }

        override fun toString(): String {
            return "Element(dato=$dato, beløp=$beløp)"
        }

        companion object {
            fun tilTidslinje(): Tidslinje<Beløp> {
                val siste = grunnbeløpene.last()

                return grunnbeløpene
                    .zipWithNext { gjeldende, neste ->
                        val periode = Periode(gjeldende.dato, neste.dato.minusDays(1))
                        Segment(periode, gjeldende.beløp)
                    }
                    .plus(Segment(Periode(siste.dato, LocalDate.MAX), siste.beløp))
                    .let(::Tidslinje)
            }
        }
    }

    private class GjennomsnittElement private constructor(
        private val år: Year,
        private val gjennomsnittBeløp: Beløp
    ) : Comparable<GjennomsnittElement> {

        constructor(år: Int, gjennomsnittBeløp: Int) : this(
            år = Year.of(år),
            gjennomsnittBeløp = Beløp(gjennomsnittBeløp)
        )

        override fun compareTo(other: GjennomsnittElement): Int {
            return this.år.compareTo(other.år)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as GjennomsnittElement

            return år == other.år
        }

        override fun hashCode(): Int {
            return år.hashCode()
        }

        override fun toString(): String {
            return "GjennomsnittElement(dato=$år, gjennomsnittBeløp=$gjennomsnittBeløp)"
        }

        companion object {
            fun finnGUnit(dato: LocalDate, beløp: Beløp): BenyttetGjennomsnittsbeløp {
                val grunnbeløp =
                    priv_tilTidslinjeGjennomsnitt().segment(dato)?.verdi
                        ?: throw RuntimeException("Finner ikke gjennomsnittsbeløp for dato: $dato.")

                return BenyttetGjennomsnittsbeløp(
                    år = Year.of(dato.year),
                    beløp = grunnbeløp.gjennomsnittBeløp,
                    gUnit = GUnit(beløp.dividert(grunnbeløp.gjennomsnittBeløp, GUnit.SCALE))
                )
            }

            fun finnGUnit(år: Year, beløp: Beløp): BenyttetGjennomsnittsbeløp {
                return finnGUnit(år.atDay(1), beløp)
            }

            fun tilTidslinjeGjennomsnitt(): Tidslinje<Beløp> {
                return priv_tilTidslinjeGjennomsnitt().mapValue(GjennomsnittElement::gjennomsnittBeløp)
            }

            private fun priv_tilTidslinjeGjennomsnitt(): Tidslinje<GjennomsnittElement> {
                val siste = gjennomsnittsbeløpene.last()

                return gjennomsnittsbeløpene
                    .zipWithNext { gjeldende, neste ->
                        val periode =
                            Periode(gjeldende.år.atDay(1), neste.år.atDay(1).minusDays(1))
                        Segment(periode, gjeldende)
                    }
                    .plus(Segment(Periode(siste.år.atDay(1), LocalDate.MAX), siste))
                    .let(::Tidslinje)
            }
        }
    }

    class BenyttetGjennomsnittsbeløp(
        val år: Year,
        val beløp: Beløp,
        val gUnit: GUnit
    )

    fun finnGUnit(år: Year, beløp: Beløp): BenyttetGjennomsnittsbeløp {
        return GjennomsnittElement.finnGUnit(år, beløp)
    }

    fun finnGUnit(dato: LocalDate, beløp: Beløp): BenyttetGjennomsnittsbeløp {
        return GjennomsnittElement.finnGUnit(dato, beløp)
    }

    fun tilTidslinje(): Tidslinje<Beløp> {
        return Element.tilTidslinje()
    }

    fun tilTidslinjeGjennomsnitt(): Tidslinje<Beløp> {
        return GjennomsnittElement.tilTidslinjeGjennomsnitt()
    }
}
